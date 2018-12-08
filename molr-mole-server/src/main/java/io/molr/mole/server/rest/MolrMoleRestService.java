package io.molr.mole.server.rest;

import io.molr.commons.domain.Mission;
import io.molr.commons.domain.MissionHandle;
import io.molr.commons.domain.Strand;
import io.molr.commons.domain.StrandCommand;
import io.molr.commons.domain.dto.*;
import io.molr.mole.core.api.Mole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static io.molr.mole.core.api.MoleWebApi.MISSION_REPRESENTATION_MISSION_NAME;
import static io.molr.mole.core.api.MoleWebApi.MISSION_REPRESENTATION_PATH;

@RestController
public class MolrMoleRestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MolrMoleRestService.class);

    private final Mole mole;

    public MolrMoleRestService(Mole mole) {
        this.mole = mole;
    }

    /*
        GET mappings
     */

    @GetMapping(path = "/states", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<AgencyStateDto> states() {
        return mole.states().map(AgencyStateDto::from);
    }

    @GetMapping(path = MISSION_REPRESENTATION_PATH)
    public Mono<MissionRepresentationDto> representationOf(@PathVariable(MISSION_REPRESENTATION_MISSION_NAME) String missionName) {
        return mole.representationOf(new Mission(missionName)).map(MissionRepresentationDto::from);
    }

    @GetMapping(path = "/mission/{missionName}/parameterDescription")
    public Mono<MissionParameterDescriptionDto> parameterDescriptionOf(@PathVariable("missionName") String missionName) {
        return mole.parameterDescriptionOf(new Mission(missionName)).map(MissionParameterDescriptionDto::from);
    }

    @GetMapping(path = "/instance/{missionHandle}/states", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<MissionStateDto> statesFor(@PathVariable("missionHandle") String missionHandle) {
        return mole.statesFor(MissionHandle.ofId(missionHandle)).map(MissionStateDto::from);
    }

    @GetMapping(path = "/instance/{missionHandle}/outputs", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<MissionOutputDto> outputsFor(@PathVariable("missionHandle") String missionHandle) {
        return mole.outputsFor(MissionHandle.ofId(missionHandle)).map(MissionOutputDto::from);
    }

    @GetMapping(path = "/instance/{missionHandle}/representations", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<MissionRepresentationDto> representationsFor(@PathVariable("missionHandle") String missionHandle) {
        return mole.representationsFor(MissionHandle.ofId(missionHandle)).map(MissionRepresentationDto::from);
    }

    @GetMapping(path = "/test-stream/{count}", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<TestValueDto> testResponse(@PathVariable("count") int count) {
        return Flux.interval(Duration.of(1, ChronoUnit.SECONDS))
                .take(count)
                .map(i -> new TestValueDto("Test output " + i));
    }

    /*
        POST mappings
     */

    @PostMapping(path = "/mission/{missionName}/instantiate")
    public Mono<MissionHandleDto> instantiate(@PathVariable("missionName") String missionName, @RequestBody Map<String, Object> params) {
        return mole.instantiate(new Mission(missionName), params).map(MissionHandleDto::from);
    }

    @PostMapping(path = "/instance/{missionHandle}/{strandId}/instruct/{commandName}")
    public void instruct(@PathVariable("missionHandle") String missionHandle, @PathVariable("strandId") String strandId, @PathVariable("commandName") String commandName) {
        mole.instruct(MissionHandle.ofId(missionHandle), Strand.ofId(strandId), StrandCommand.valueOf(commandName));
    }

    @PostMapping(path = "/instance/{missionHandle}/instructRoot/{commandName}")
    public void instructRoot(@PathVariable("missionHandle") String missionHandle, @PathVariable("commandName") String commandName) {
        mole.instructRoot(MissionHandle.ofId(missionHandle), StrandCommand.valueOf(commandName));
    }

    @ExceptionHandler({Exception.class})
    public @ResponseBody
    ResponseEntity<?> handleException(Exception e) {
        LOGGER.error("Error: {}", e.getMessage(), e);
        return ResponseEntity.badRequest().body(e.getMessage());
    }

}