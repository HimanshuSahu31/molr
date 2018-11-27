package org.molr.mole.core.runnable;

import org.molr.commons.domain.Mission;
import org.molr.commons.domain.MissionInput;
import org.molr.commons.domain.MissionParameterDescription;
import org.molr.commons.domain.MissionRepresentation;
import org.molr.commons.domain.Result;
import org.molr.commons.domain.RunState;
import org.molr.mole.core.runnable.exec.RunnableBlockExecutor;
import org.molr.mole.core.tree.AbstractJavaMole;
import org.molr.mole.core.tree.ConcurrentMissionOutputCollector;
import org.molr.mole.core.tree.LeafExecutor;
import org.molr.mole.core.tree.MissionExecutor;
import org.molr.mole.core.tree.MissionOutputCollector;
import org.molr.mole.core.tree.TreeMissionExecutor;
import org.molr.mole.core.tree.TreeStructure;
import org.molr.mole.core.tree.tracking.TreeTracker;

import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class RunnableLeafsMole extends AbstractJavaMole {

    private final Map<Mission, RunnableLeafsMission> missions;

    public RunnableLeafsMole(Set<RunnableLeafsMission> missions) {
        this.missions = createMissionsMap(requireNonNull(missions, "missions must not be null"));
    }

    private Map<Mission, RunnableLeafsMission> createMissionsMap(Set<RunnableLeafsMission> newMissions) {
        return newMissions.stream()
                .collect(toMap(m -> new Mission(m.name()), identity()));
    }

    @Override
    public Set<Mission> availableMissions() {
        return missions.keySet();
    }

    @Override
    public MissionRepresentation representationOf(Mission mission) {
        return getOrThrow(mission).treeStructure().missionRepresentation();
    }

    @Override
    public MissionParameterDescription parameterDescriptionOf(Mission mission) {
        return getOrThrow(mission).parameterDescription();
    }


    private RunnableLeafsMission getOrThrow(Mission mission) {
        RunnableLeafsMission runnableMission = missions.get(mission);
        if (runnableMission == null) {
            throw new IllegalArgumentException(mission + " is not a mission of this mole");
        }
        return runnableMission;
    }


    @Override
    protected MissionExecutor instantiate(Mission mission, Map<String, Object> params) {
        RunnableLeafsMission runnableLeafMission = missions.get(mission);
        TreeStructure treeStructure = runnableLeafMission.treeStructure();
        TreeTracker<Result> resultTracker = TreeTracker.create(treeStructure.missionRepresentation(), Result.UNDEFINED, Result::summaryOf);
        TreeTracker<RunState> runStateTracker = TreeTracker.create(treeStructure.missionRepresentation(), RunState.UNDEFINED, RunState::summaryOf);

        MissionOutputCollector outputCollector = new ConcurrentMissionOutputCollector();

        LeafExecutor leafExecutor = new RunnableBlockExecutor(resultTracker, runnableLeafMission.runnables(), MissionInput.from(params), outputCollector, runStateTracker);
        return new TreeMissionExecutor(treeStructure, leafExecutor, resultTracker, outputCollector, runStateTracker);
    }
}
