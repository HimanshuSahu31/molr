package org.molr.mole.remote.rest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.molr.commons.domain.*;
import org.molr.mole.core.api.Mole;
import org.molr.mole.core.tree.BlockOutputCollector;
import org.molr.mole.core.tree.ConcurrentMissionOutputCollector;
import org.molr.mole.core.tree.MissionOutputCollector;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.molr.commons.domain.Placeholder.aString;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = DEFINED_PORT)
@ContextConfiguration(classes = MolrMoleRestService.class)
@EnableAutoConfiguration
public class RestRemoteMoleTest {

    public static final Block BLOCK2 = Block.idAndText("blockId2", "text");
    public static final Block BLOCK1 = Block.idAndText("blockId1", "text");
    public static final Strand STRAND1 = Strand.ofId("strandId");
    public static final Strand STRAND2 = Strand.ofId("strandId2");


    private final String baseUrl = "http://localhost:8800";

    @MockBean
    private Mole mole;

    @Before
    public void setUpMole() {
        Set<Mission> missions = new HashSet<>();
        missions.add(new Mission("run a Marathon"));
        missions.add(new Mission("swim 10km"));
        MissionParameter<?> param = MissionParameter.required(aString("testValue")).withDefault("Test parameter");
        MissionParameterDescription parameterDescription = new MissionParameterDescription(Collections.singleton(param));
        when(mole.parameterDescriptionOf(any(Mission.class))).thenReturn(parameterDescription);

        when(mole.availableMissions()).thenReturn(missions);
        when(mole.representationOf(any(Mission.class))).thenReturn(ImmutableMissionRepresentation.empty("anEmpty representation"));

        MissionState.Builder builder = MissionState.builder(Result.SUCCESS);
        builder.add(STRAND1, RunState.PAUSED, BLOCK1, Collections.singleton(StrandCommand.RESUME));
        builder.add(STRAND2, RunState.FINISHED, BLOCK2, STRAND1,Collections.emptySet());
        builder.blockResult(BLOCK2, Result.SUCCESS);
        builder.blockRunState(BLOCK2, RunState.FINISHED);
        Flux<MissionState> statesFlux = Flux.fromIterable(Collections.singleton(builder.build()));
        when(mole.statesFor(any(MissionHandle.class))).thenReturn(statesFlux);

        MissionOutputCollector outputCollector = new ConcurrentMissionOutputCollector();
        outputCollector.put(BLOCK1,"example","this is an output example");
        outputCollector.put(BLOCK2,"another example","this is another output example");
        Flux<MissionOutput> outputValue = outputCollector.asStream();
        when(mole.outputsFor(any(MissionHandle.class))).thenReturn(outputValue);

    }


    @Test
    public void availableMissions() {
        RestRemoteMole remoteMole = new RestRemoteMole(baseUrl);
        Set<Mission> missions = remoteMole.availableMissions();
        assertThat(missions.size()).isEqualTo(2);
        assertThat(missions.contains(new Mission("run a Marathon"))).isTrue();
    }

    @Test (expected = IllegalArgumentException.class)
    public void availableMissionsReturnsAnError() {
        RestRemoteMole remoteMole = new RestRemoteMole(baseUrl);
        when(mole.availableMissions()).thenThrow(new IllegalArgumentException("the mole is returning an error"));
        Set<Mission> missions = remoteMole.availableMissions();
    }

    @Test
    public void representationOf() {
        RestRemoteMole remoteMole = new RestRemoteMole(baseUrl);
        MissionRepresentation rep = remoteMole.representationOf(new Mission("Linear Mission"));
        assertThat(rep.parentsToChildren().isEmpty()).isTrue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void representationOfReturnsError() {
        RestRemoteMole remoteMole = new RestRemoteMole(baseUrl);
        when(mole.representationOf(any())).thenThrow(new IllegalArgumentException("the mole is returning an error"));
        MissionRepresentation rep = remoteMole.representationOf(new Mission("Linear Mission"));
    }

    @Test
    public void parameterDescriptionOf() {
        RestRemoteMole remoteMole = new RestRemoteMole(baseUrl);
        MissionParameterDescription parameterDescription = remoteMole.parameterDescriptionOf(new Mission("run a Marathon"));
        Optional<MissionParameter<?>> parameter = parameterDescription.parameters().stream().findFirst();
        assertThat(parameter.isPresent()).isTrue();
        assertThat(parameter.get().isRequired()).isTrue();
        assertThat(parameter.get().defaultValue()).isEqualTo("Test parameter");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parameterDescriptionReturnsError() {
        RestRemoteMole remoteMole = new RestRemoteMole(baseUrl);
        when(mole.parameterDescriptionOf(any())).thenThrow(new IllegalArgumentException("the mole is returning an error"));
        MissionParameterDescription parameterDescription = remoteMole.parameterDescriptionOf(new Mission("run a Marathon"));
    }

    @Test
    public void instantiate() {
        RestRemoteMole remoteMole = new RestRemoteMole(baseUrl);
        Map<String,Object> params = new HashMap<>();
        params.put("paramName" , "param desc");
        remoteMole.instantiate(MissionHandle.ofId("missionId"), new Mission("a mission"), params);
        Mockito.verify(mole,Mockito.atLeastOnce()).instantiate(any(),any(),any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void instantiateReturnsError() {
        RestRemoteMole remoteMole = new RestRemoteMole(baseUrl);
        doThrow(new IllegalArgumentException("the mole is returning an error"))
                .when(mole).instantiate(any(),any(),any());
        remoteMole.instantiate(MissionHandle.ofId("missionId"), new Mission("a mission"), Collections.emptyMap());
    }

    @Test
    public void statesFor() {
        RestRemoteMole remoteMole = new RestRemoteMole(baseUrl);
        Flux<MissionState> states = remoteMole.statesFor(MissionHandle.ofId("missionHandleId"));
        Set<MissionState> statesSet = states.collect(Collectors.toSet()).block();
        MissionState missionState = statesSet.iterator().next();
        RunState runStateForStrand = missionState.runStateOf(Strand.ofId("strandId"));
        RunState runStateForBlock = missionState.runStateOf(BLOCK2);
        Result blockResult = missionState.resultOf(BLOCK2);

        assertThat(runStateForStrand).isEqualTo(RunState.PAUSED);
        assertThat(runStateForBlock).isEqualTo(RunState.FINISHED);
        assertThat(blockResult).isEqualTo(Result.SUCCESS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void statesForReturnsError() {
        RestRemoteMole remoteMole = new RestRemoteMole(baseUrl);
        when(mole.statesFor(any())).thenThrow(new IllegalArgumentException("the mole is returning an error"));
        Flux<MissionState> states = remoteMole.statesFor(MissionHandle.ofId("anything"));
    }

    @Test
    public void outputsFor(){
        RestRemoteMole remoteMole = new RestRemoteMole(baseUrl);
        Flux<MissionOutput> outputs = remoteMole.outputsFor(MissionHandle.ofId("missionHandleId"));
        assertThat(outputs.hasElements().block()).isTrue();
        System.out.println(outputs.blockFirst().get(BLOCK1, aString("example")));
        assertThat(outputs.blockFirst().get(BLOCK1, aString("example"))).isEqualTo("this is an output example");
    }

    @Test(expected = IllegalArgumentException.class)
    public void outputsForReturnsError() {
        RestRemoteMole remoteMole = new RestRemoteMole(baseUrl);
        when(mole.outputsFor(any())).thenThrow(new IllegalArgumentException("the mole is returning an error"));
        Flux<MissionOutput> outputs = remoteMole.outputsFor(MissionHandle.ofId("anything"));
    }

    @Test
    public void instruct(){
        RestRemoteMole remoteMole = new RestRemoteMole(baseUrl);
        remoteMole.instruct(MissionHandle.ofId("missionId"),Strand.ofId("strandId"),StrandCommand.RESUME);
        Mockito.verify(mole,Mockito.atLeastOnce()).instruct(any(),any(),any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void instructReturnsError(){
        RestRemoteMole remoteMole = new RestRemoteMole(baseUrl);
        doThrow(new IllegalArgumentException("the mole is returning an error"))
                .when(mole).instruct(any(),any(),any());
        remoteMole.instruct(MissionHandle.ofId("missionId"), Strand.ofId("strandId"),StrandCommand.RESUME);
    }

    @Test(expected = Exception.class)
    public void testWrongUriThrows(){
        RestRemoteMole remoteMole = new RestRemoteMole("http://wrongUri");
        Set<Mission> missions = remoteMole.availableMissions();
    }


}