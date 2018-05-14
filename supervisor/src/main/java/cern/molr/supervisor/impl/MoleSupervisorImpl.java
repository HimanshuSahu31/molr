package cern.molr.supervisor.impl;

import cern.molr.commons.response.CommandResponse;
import cern.molr.exception.UnknownMissionException;
import cern.molr.mission.Mission;
import cern.molr.mole.spawner.run.RunEvents;
import cern.molr.mole.spawner.run.RunSpawner;
import cern.molr.mole.supervisor.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * Implementation of {@link MoleSupervisor} which manage a mission execution
 * @author yassine
 */
public class MoleSupervisorImpl implements MoleSupervisor {

    protected SupervisorSessionsManager sessionsManager=new SupervisorSessionsManagerImpl();

    @Override
    public <I> Flux<MoleExecutionEvent> instantiate(Mission mission, I args, String missionExecutionId){
        try {
            MoleSession session;
            RunSpawner<I> spawner=new RunSpawner<>();
            session=spawner.spawnMoleRunner(mission,args);
            sessionsManager.addSession(missionExecutionId,session);
            session.getController().addMoleExecutionListener((event)->{
                if(event instanceof RunEvents.JVMDestroyed) {
                    sessionsManager.removeSession(session);
                    try {
                        session.getController().close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            return Flux.create((FluxSink<MoleExecutionEvent> emitter)->{
                session.getController().addMoleExecutionListener((event)->{
                    emitter.next(event);
                    if(event instanceof RunEvents.JVMDestroyed)
                        emitter.complete();
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
            return Flux.just(new RunEvents.MissionException(e));
        }
    }

    @Override
    public Mono<MoleExecutionCommandResponse> instruct(MoleExecutionCommand command) {
        return Mono.just(sessionsManager.getSession(command.getMissionId()).map((session)->session.getController().sendCommand(command)).orElse(new CommandResponse.CommandResponseFailure(new UnknownMissionException("Mission not found in supervisor registry"))));
    }
}
