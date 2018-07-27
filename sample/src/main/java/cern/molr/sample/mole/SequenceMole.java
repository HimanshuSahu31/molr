/*
 * © Copyright 2016 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file “COPYING“. In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */

package cern.molr.sample.mole;


import cern.molr.commons.api.exception.CommandNotAcceptedException;
import cern.molr.commons.api.exception.IncompatibleMissionException;
import cern.molr.commons.api.exception.MissionExecutionException;
import cern.molr.commons.api.exception.MissionResolvingException;
import cern.molr.commons.api.mission.Mission;
import cern.molr.commons.api.mission.Mole;
import cern.molr.commons.api.request.MissionCommand;
import cern.molr.commons.api.response.MissionEvent;
import cern.molr.commons.api.response.MissionState;
import cern.molr.commons.impl.mission.MissionServices;
import cern.molr.sample.commands.SequenceCommand;
import cern.molr.sample.events.SequenceMissionEvent;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Implementation of {@link Mole} which allows for the execution of classes implementing the {@link SequenceMission}
 * interface
 *
 * It runs the tasks of the mission consecutively
 *
 * @author yassine-kr
 * @see Mole
 */
public class SequenceMole implements Mole<Void, Void> {

    private List<Runnable> tasks;
    private int currentTask = 0;
    private CountDownLatch endSignal = new CountDownLatch(1);
    private boolean running = false;
    private Processor<MissionEvent, MissionEvent> eventsProcessor = DirectProcessor.create();

    @Override
    public void verify(String missionName) throws IncompatibleMissionException {
        Class<?> classType = null;
        try {
            classType = MissionServices.getResolver().resolve(missionName);
        } catch (MissionResolvingException error) {
            throw new IncompatibleMissionException(error);
        }

        if (null == classType) {
            throw new IllegalArgumentException("Class type cannot be null");
        }
        if (SequenceMission.class.isAssignableFrom(classType)) {
            try {
                classType.getMethod("getTasks");
            } catch (NoSuchMethodException error) {
                throw new IncompatibleMissionException(error);
            }
        } else
            throw new IncompatibleMissionException("Mission must implement SequenceMission interface");
    }

    @Override
    public Void run(Mission mission, Void missionArguments) throws MissionExecutionException {
        try {
            Class<?> missionClass = MissionServices.getResolver().resolve(mission.getMissionName());
            Object missionInstance = missionClass.getConstructor().newInstance();
            if (!(missionInstance instanceof SequenceMission)) {
                throw new IllegalArgumentException(String
                        .format("Mission content class must implement the %s interface", SequenceMission.class.getName()));
            }
            tasks = ((SequenceMission) missionInstance).getTasks();
            if (tasks == null || tasks.size() == 0) {
                throw new IllegalArgumentException("Null or Empty tasks list");
            }
            endSignal.await();
        } catch (Exception error) {
            throw new MissionExecutionException(error);
        }
        return null;
    }

    @Override
    public void sendCommand(MissionCommand command) throws CommandNotAcceptedException {
        if (!(command instanceof SequenceCommand)) {
            throw new CommandNotAcceptedException("Command not accepted by the Mole; it is not a known a command by " +
                    "the mole");
        }

        if (running || endSignal.getCount() == 0) {
            throw new CommandNotAcceptedException("Command not accepted by the Mole; the mission is running or " +
                    "finished");
        }

        switch (((SequenceCommand) command).getCommand()) {
             case STEP:
                 new Thread(this::runTask).start();
                 break;
            case SKIP:
                nextTask();
                break;
            case FINISH:
                new Thread(() -> {
                    for (; currentTask < tasks.size();) {
                        runTask();
                    }
                }).start();
                break;
        }
    }

    private void nextTask() {
        currentTask++;
        if (currentTask == tasks.size()) {
            endSignal.countDown();
        }
    }

    private void runTask() {
        running = true;
        eventsProcessor.onNext(new SequenceMissionEvent(currentTask,SequenceMissionEvent.Event.TASK_STARTED));
        tasks.get(currentTask).run();
        running = false;
        eventsProcessor.onNext(new SequenceMissionEvent(currentTask,SequenceMissionEvent.Event.TASK_FINISHED));
        nextTask();
    }

    @Override
    public Publisher<MissionEvent> getEventsPublisher() {
        return eventsProcessor;
    }

    @Override
    public Publisher<MissionState> getStatesPublisher() {
        return null;
    }

}