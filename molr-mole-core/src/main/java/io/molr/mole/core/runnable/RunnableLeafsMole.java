package io.molr.mole.core.runnable;

import io.molr.commons.domain.*;
import io.molr.mole.core.runnable.exec.RunnableBlockExecutor;
import io.molr.mole.core.tree.*;
import io.molr.mole.core.tree.tracking.TreeTracker;

import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

public class RunnableLeafsMole extends AbstractJavaMole {

    public RunnableLeafsMole(Set<RunnableLeafsMission> missions) {
        super(extractMissions(missions));
    }

    private static Set<Mission> extractMissions(Set<RunnableLeafsMission> missions) {
        requireNonNull(missions, "missions must not be null");
        return missions.stream().map(rlm -> (Mission) rlm).collect(toSet());
    }

    @Override
    public MissionRepresentation missionRepresentationOf(Mission mission) {
        return getOrThrow(mission).treeStructure().missionRepresentation();
    }

    @Override
    public MissionParameterDescription missionParameterDescriptionOf(Mission mission) {
        return getOrThrow(mission).parameterDescription();
    }


    private RunnableLeafsMission getOrThrow(Mission mission) {
        if (mission instanceof RunnableLeafsMission) {
            return (RunnableLeafsMission) mission;
        } else {
            throw new IllegalArgumentException(mission + " is not a mission of this mole");
        }
    }


    @Override
    protected MissionExecutor executorFor(Mission mission, Map<String, Object> params) {
        RunnableLeafsMission runnableLeafMission = (RunnableLeafsMission) mission;
        TreeStructure treeStructure = runnableLeafMission.treeStructure();
        TreeTracker<Result> resultTracker = TreeTracker.create(treeStructure.missionRepresentation(), Result.UNDEFINED, Result::summaryOf);
        TreeTracker<RunState> runStateTracker = TreeTracker.create(treeStructure.missionRepresentation(), RunState.UNDEFINED, RunState::summaryOf);

        MissionOutputCollector outputCollector = new ConcurrentMissionOutputCollector();

        LeafExecutor leafExecutor = new RunnableBlockExecutor(resultTracker, runnableLeafMission.runnables(), MissionInput.from(params), outputCollector, runStateTracker);
        return new TreeMissionExecutor(treeStructure, leafExecutor, resultTracker, outputCollector, runStateTracker);
    }

}
