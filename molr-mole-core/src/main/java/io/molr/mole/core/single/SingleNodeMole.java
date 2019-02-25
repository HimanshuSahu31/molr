package io.molr.mole.core.single;

import io.molr.commons.domain.Mission;
import io.molr.commons.domain.MissionParameterDescription;
import io.molr.commons.domain.MissionRepresentation;
import io.molr.mole.core.tree.AbstractJavaMole;
import io.molr.mole.core.tree.MissionExecutor;

import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

public class SingleNodeMole extends AbstractJavaMole {

    public SingleNodeMole(Set<SingleNodeMission<?>> singleLeafMissions) {
        super(extractMissions(singleLeafMissions));
    }

    private static Set<Mission> extractMissions(Set<SingleNodeMission<?>> missions) {
        requireNonNull(missions, "missions must not be null.");
        return missions.stream().map(snm -> (Mission) snm).collect(toSet());
    }

    @Override
    public MissionRepresentation missionRepresentationOf(Mission mission) {
        SingleNodeMission singleNodeMission = (SingleNodeMission) mission;
        return SingleNodeMissions.representationFor(singleNodeMission);
    }


    @Override
    public MissionParameterDescription missionParameterDescriptionOf(Mission mission) {
        SingleNodeMission singleNodeMission = (SingleNodeMission) mission;
        return singleNodeMission.parameterDescription();
    }

    @Override
    protected MissionExecutor executorFor(Mission mission, Map<String, Object> params) {
        SingleNodeMission<?> singleNodeMission = (SingleNodeMission) mission;
        return new SingleNodeMissionExecutor<>(singleNodeMission, params);
    }

}
