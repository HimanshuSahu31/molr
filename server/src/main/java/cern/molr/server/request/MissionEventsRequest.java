/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package cern.molr.server.request;

/**
 * Request to get strem of events from server
 */
public class MissionEventsRequest {

    private String missionExecutionId;

    public MissionEventsRequest(){}

    public MissionEventsRequest(String missionExecutionId){
        this.setMissionExecutionId(missionExecutionId);
    }

    public String getMissionExecutionId() {
        return missionExecutionId;
    }

    public void setMissionExecutionId(String missionExecutionId) {
        this.missionExecutionId = missionExecutionId;
    }
    
}
