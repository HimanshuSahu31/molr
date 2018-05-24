/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package cern.molr.server;

import cern.molr.commons.request.supervisor.SupervisorRegisterRequest;
import cern.molr.commons.request.supervisor.SupervisorUnregisterRequest;
import cern.molr.commons.response.*;
import cern.molr.commons.response.MissionExecutionResponse.MissionExecutionResponseFailure;
import cern.molr.commons.response.MissionExecutionResponse.MissionExecutionResponseSuccess;
import cern.molr.commons.response.SupervisorRegisterResponse.SupervisorRegisterResponseFailure;
import cern.molr.commons.response.SupervisorRegisterResponse.SupervisorRegisterResponseSuccess;
import cern.molr.commons.response.SupervisorUnregisterResponse.SupervisorUnregisterResponseFailure;
import cern.molr.commons.response.SupervisorUnregisterResponse.SupervisorUnregisterResponseSuccess;
import cern.molr.exception.MissionExecutionNotAccepted;
import cern.molr.exception.NoAppropriateSupervisorFound;
import cern.molr.server.request.ServerMissionExecutionRequest;
import cern.molr.type.Ack;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

/**
 * {@link RestController} for {@link ServerMain} spring application
 * 
 * @author nachivpn
 * @author yassine-kr
 */
@RestController
public class ServerRestController {

    private final ServerRestExecutionService gateway;

    public ServerRestController(ServerRestExecutionService gateway) {
        this.gateway = gateway;
    }


    @RequestMapping(path = "/instantiate", method = RequestMethod.POST)
    public <I> CompletableFuture<MissionExecutionResponse> instantiateMission(@RequestBody ServerMissionExecutionRequest<I> request) {
        return CompletableFuture.<MissionExecutionResponse>supplyAsync(()->{
            try {
                String mEId = gateway.instantiate(request.getMissionDefnClassName(), request.getArgs());
                return new MissionExecutionResponseSuccess(new MissionExecutionResponseBean(mEId));
            } catch (MissionExecutionNotAccepted |NoAppropriateSupervisorFound e) {
                return new MissionExecutionResponseFailure(e);
            }
        },Executors.newSingleThreadExecutor());
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public CompletableFuture<SupervisorRegisterResponse> register(@RequestBody SupervisorRegisterRequest request) {
        try {
            return CompletableFuture.<SupervisorRegisterResponse>supplyAsync(() ->{
                String id= gateway.addSupervisor(request.getHost(),request.getPort(),request.getAcceptedMissions());
                return new SupervisorRegisterResponseSuccess(new SupervisorRegisterResponseBean(id));
            }).exceptionally(SupervisorRegisterResponseFailure::new);
        } catch (Exception e) {
            return CompletableFuture.supplyAsync(() -> new SupervisorRegisterResponseFailure(e));
        }
    }

    @RequestMapping(path = "/unregister", method = RequestMethod.POST)
    public CompletableFuture<SupervisorUnregisterResponse> uNregister(@RequestBody SupervisorUnregisterRequest request) {
        try {
            return CompletableFuture.<SupervisorUnregisterResponse>supplyAsync(() ->{
                gateway.removeSupervisor(request.getId());
                return new SupervisorUnregisterResponseSuccess(new Ack("Supervisor unregistered successfully"));
            }).exceptionally(SupervisorUnregisterResponseFailure::new);
        } catch (Exception e) {
            return CompletableFuture.supplyAsync(() -> new SupervisorUnregisterResponseFailure(e));
        }
    }


}
