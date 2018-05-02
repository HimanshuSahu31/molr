/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package cern.molr.server;

import java.util.concurrent.CompletableFuture;

import cern.molr.commons.response.*;
import cern.molr.exception.NoAppropriateSupervisorFound;
import cern.molr.commons.request.supervisor.SupervisorRegisterRequest;
import cern.molr.commons.request.supervisor.SupervisorUnregisterRequest;
import cern.molr.server.supervisor.StatefulMoleSupervisorProxy;
import cern.molr.server.supervisor.StatefulMoleSupervisorProxyNew;
import cern.molr.server.supervisor.SupervisorsManagerImpl;
import cern.molr.server.supervisor.SupervisorsManagerImplNew;
import cern.molr.type.Ack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import cern.molr.commons.response.MissionCancelResponse.MissionCancelResponseFailure;
import cern.molr.commons.response.MissionCancelResponse.MissionCancelResponseSuccess;
import cern.molr.commons.response.MissionExecutionResponse.MissionExecutionResponseFailure;
import cern.molr.commons.response.MissionExecutionResponse.MissionExecutionResponseSuccess;
import cern.molr.commons.response.MissionGenericResponse.MissionGenericResponseFailure;
import cern.molr.commons.response.MissionGenericResponse.MissionGenericResponseSuccess;
import cern.molr.commons.response.SupervisorRegisterResponse.SupervisorRegisterResponseSuccess;
import cern.molr.commons.response.SupervisorRegisterResponse.SupervisorRegisterResponseFailure;
import cern.molr.exception.UnknownMissionException;
import cern.molr.server.request.MissionCancelRequest;
import cern.molr.server.request.MissionExecutionRequest;
import cern.molr.server.request.MissionResultRequest;
import cern.molr.commons.response.SupervisorUnregisterResponse.SupervisorUnregisterResponseSuccess;
import cern.molr.commons.response.SupervisorUnregisterResponse.SupervisorUnregisterResponseFailure;

/**
 * {@link RestController} for {@link ServerMain} spring application
 * 
 * @author nachivpn
 * @author yassine
 */
@RestController
public class ServerRestController {

    //TODO should be removed
    private final ServerRestExecutionService meGateway;

    //TODO should be removed
    private SupervisorsManager supervisorsManager=new SupervisorsManagerImpl();

    private final ServerRestExecutionServiceNew newGateway;
    private final SupervisorsManagerNew newSupervisorsManager;

    public ServerRestController(ServerRestExecutionService meGateway,ServerRestExecutionServiceNew newGateway,SupervisorsManagerNew newSupervisorsManager) {
        this.meGateway = meGateway;
        this.meGateway.setSupervisorsManager(supervisorsManager);

        this.newGateway=newGateway;
        this.newSupervisorsManager=newSupervisorsManager;
    }


    @RequestMapping(path = "/instantiate", method = RequestMethod.POST)
    public <I> MissionExecutionResponse instantiateMission(@RequestBody MissionExecutionRequest<I> request) {
        try {
            String mEId = newGateway.instantiate(request.getMissionDefnClassName(), request.getArgs());
            return new MissionExecutionResponseSuccess(new MissionExecutionResponseBean(mEId));
        } catch (UnknownMissionException|NoAppropriateSupervisorFound e) {
            return new MissionExecutionResponseFailure(e);
        }

    }

    //TODO remove "New" from method name
    @RequestMapping(path = "/registerNew", method = RequestMethod.POST)
    public CompletableFuture<SupervisorRegisterResponse> registerNew(@RequestBody SupervisorRegisterRequest request) {
        try {
            return CompletableFuture.<SupervisorRegisterResponse>supplyAsync(() ->{
                StatefulMoleSupervisorNew moleSupervisor = new StatefulMoleSupervisorProxyNew(request.getHost(),request.getPort());
                String id=newSupervisorsManager.addSupervisor(moleSupervisor,request.getAcceptedMissions());
                return new SupervisorRegisterResponseSuccess(new SupervisorRegisterResponseBean(id));
            }).exceptionally(SupervisorRegisterResponseFailure::new);
        } catch (Exception e) {
            return CompletableFuture.supplyAsync(() -> new SupervisorRegisterResponseFailure(e));
        }
    }

    //TODO remove "New" from method name
    @RequestMapping(path = "/unregisterNew", method = RequestMethod.POST)
    public CompletableFuture<SupervisorUnregisterResponse> uNregisterNew(@RequestBody SupervisorUnregisterRequest request) {
        try {
            return CompletableFuture.<SupervisorUnregisterResponse>supplyAsync(() ->{
                newSupervisorsManager.removeSupervisor(request.getId());
                return new SupervisorUnregisterResponseSuccess(new Ack("Supervisor unregistered successfully"));
            }).exceptionally(SupervisorUnregisterResponseFailure::new);
        } catch (Exception e) {
            return CompletableFuture.supplyAsync(() -> new SupervisorUnregisterResponseFailure(e));
        }
    }

    //TODO should be removed
    @RequestMapping(path = "/mission", method = RequestMethod.POST)
    public <I> MissionExecutionResponse newMission(@RequestBody MissionExecutionRequest<I> request) {
        try {
            String mEId = meGateway.runMission(request.getMissionDefnClassName(), request.getArgs());
            return new MissionExecutionResponseSuccess(new MissionExecutionResponseBean(mEId));
        } catch (UnknownMissionException|NoAppropriateSupervisorFound e) {
            /*
             * The idea behind not catching Exception is to ensure that any server related 
             * issues are not off-loaded to be handled by the client (same applies below)
             * Moreover, the Molr API does NOT define any runtime exceptions 
             * and catching any would be analogous to suppressing an issue!
             */
            return new MissionExecutionResponseFailure(e);
        }

    }

    //TODO should be removed
    @RequestMapping(path = "/result", method = RequestMethod.POST)
    public CompletableFuture<MissionGenericResponse<Object>> result(@RequestBody MissionResultRequest request) {
        try {
            return meGateway.getResult(request.getMissionExecutionId())
                    .<MissionGenericResponse<Object>>thenApply(MissionGenericResponseSuccess<Object>::new)
                    .exceptionally(MissionGenericResponseFailure<Object>::new);
        } catch (UnknownMissionException e) {
            return CompletableFuture.supplyAsync(() -> new MissionGenericResponseFailure<>(e));
        }
    }

    //TODO should be removed
    @RequestMapping(path = "/cancel", method = RequestMethod.POST)
    public CompletableFuture<MissionCancelResponse> cancel(@RequestBody MissionCancelRequest request) {
        try {
            return meGateway.cancel(request.getMissionExecutionId())
                    .<MissionCancelResponse>thenApply(MissionCancelResponseSuccess::new)
                    .exceptionally(MissionCancelResponseFailure::new);
        } catch (UnknownMissionException e) {
            return CompletableFuture.supplyAsync(() -> new MissionCancelResponseFailure(e));
        }
    }

    //TODO should be removed
    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public CompletableFuture<SupervisorRegisterResponse> register(@RequestBody SupervisorRegisterRequest request) {
        try {
            return CompletableFuture.<SupervisorRegisterResponse>supplyAsync(() ->{
                StatefulMoleSupervisor moleSupervisor = new StatefulMoleSupervisorProxy(request.getHost(),request.getPort());
                String id=supervisorsManager.addSupervisor(moleSupervisor,request.getAcceptedMissions());
                return new SupervisorRegisterResponseSuccess(new SupervisorRegisterResponseBean(id));
            }).exceptionally(SupervisorRegisterResponseFailure::new);
        } catch (Exception e) {
            return CompletableFuture.supplyAsync(() -> new SupervisorRegisterResponseFailure(e));
        }
    }

    //TODO should be removed
    @RequestMapping(path = "/unregister", method = RequestMethod.POST)
    public CompletableFuture<SupervisorUnregisterResponse> uNregister(@RequestBody SupervisorUnregisterRequest request) {
        try {
            return CompletableFuture.<SupervisorUnregisterResponse>supplyAsync(() ->{
                supervisorsManager.removeSupervisor(request.getId());
                return new SupervisorUnregisterResponseSuccess(new Ack("Supervisor unregistered successfully"));
            }).exceptionally(SupervisorUnregisterResponseFailure::new);
        } catch (Exception e) {
            return CompletableFuture.supplyAsync(() -> new SupervisorUnregisterResponseFailure(e));
        }
    }


}
