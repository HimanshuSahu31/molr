package cern.molr.test.supervisor.spawner;

import cern.molr.commons.api.mission.Mission;
import cern.molr.commons.api.mission.MissionMaterializer;
import cern.molr.commons.api.response.MissionEvent;
import cern.molr.commons.commands.MissionControlCommand;
import cern.molr.commons.events.MissionFinished;
import cern.molr.commons.events.MissionStateEvent;
import cern.molr.commons.impl.mission.AnnotatedMissionMaterializer;
import cern.molr.supervisor.api.session.MissionSession;
import cern.molr.supervisor.api.session.MoleController;
import cern.molr.supervisor.impl.session.ControllerImpl;
import cern.molr.supervisor.impl.spawner.JVMSpawner;
import cern.molr.test.MissionTest;
import cern.molr.test.ResponseTester;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * class for testing {@link ControllerImpl}
 *
 * @author yassine-kr
 */
public class JVMSpawnerTest {

    private <I> MoleController getController(Class<?> missionClass, I missionArguments) throws Exception {
        JVMSpawner<I> spawner = new JVMSpawner<>();
        MissionMaterializer materializer = new AnnotatedMissionMaterializer();
        Mission mission = materializer.materialize(missionClass.getName());
        MissionSession session = spawner.spawnMoleRunner(mission, missionArguments);
        return session.getController();
    }

    @Test
    public void instantiateTest() throws Exception {

        CountDownLatch signal = new CountDownLatch(1);

        MoleController controller = getController(MissionTest.class, 42);
        List<MissionEvent> events = new ArrayList<>();
        controller.addMoleExecutionListener(event -> {
            if (event instanceof MissionStateEvent) {
                return;
            }
            events.add(event);
            signal.countDown();
        });
        signal.await(1, TimeUnit.MINUTES);
        Assert.assertEquals(1, events.size());
        ResponseTester.testInstantiationEvent(events.get(0));
    }

    @Test
    public void startFinishTest() throws Exception {
        CountDownLatch signal = new CountDownLatch(4);

        MoleController controller = getController(MissionTest.class, 42);
        List<MissionEvent> events = new ArrayList<>();
        controller.addMoleExecutionListener(event -> {
            if (event instanceof MissionStateEvent) {
                return;
            }
            events.add(event);
            signal.countDown();
        });
        controller.sendCommand(new MissionControlCommand(MissionControlCommand.Command.START));
        signal.await(1, TimeUnit.MINUTES);
        Assert.assertEquals(4, events.size());
        ResponseTester.testStartedEvent(events.get(1));
        Assert.assertEquals(MissionFinished.class, events.get(2).getClass());
        ResponseTester.testTerminatedEvent(events.get(3));
        Assert.assertEquals(84, ((MissionFinished) events.get(2)).getResult());
    }

    /**
     * The mission execution should be long enough to terminate the session before the the mission is finished
     *
     * @throws Exception
     */
    @Test
    public void terminateTest() throws Exception {
        CountDownLatch signal = new CountDownLatch(3);

        MoleController controller = getController(MissionTest.class, 42);
        List<MissionEvent> events = new ArrayList<>();
        controller.addMoleExecutionListener(event -> {
            if (event instanceof MissionStateEvent) {
                return;
            }
            System.out.println(event);
            events.add(event);
            signal.countDown();
        });
        controller.sendCommand(new MissionControlCommand(MissionControlCommand.Command.START));
        controller.sendCommand(new MissionControlCommand(MissionControlCommand.Command.TERMINATE));
        signal.await(1, TimeUnit.MINUTES);
        Assert.assertEquals(3, events.size());
        ResponseTester.testTerminatedEvent(events.get(2));
    }


}