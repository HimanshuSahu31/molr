package cern.molr.server.impl;

import cern.molr.commons.api.request.MissionCommandRequest;
import cern.molr.commons.api.request.client.ServerInstantiationRequest;
import cern.molr.commons.api.response.CommandResponse;
import cern.molr.commons.api.response.MissionEvent;
import cern.molr.commons.api.response.SupervisorState;
import cern.molr.commons.api.web.MolrWebClient;
import cern.molr.commons.api.web.MolrWebSocketClient;
import cern.molr.commons.impl.web.MolrWebClientImpl;
import cern.molr.commons.impl.web.MolrWebSocketClientImpl;
import cern.molr.server.api.RemoteMoleSupervisor;
import org.reactivestreams.Publisher;

import java.util.Optional;

/**
 * Implementation of a Remote Supervisor which is able to return its state using network
 *
 * @author yassine-kr
 */
public class RemoteMoleSupervisorImpl implements RemoteMoleSupervisor {

    protected MolrWebClient client;
    private MolrWebSocketClient socketClient;

    public RemoteMoleSupervisorImpl(String host, int port) {
        this.socketClient = new MolrWebSocketClientImpl(host, port);
        this.client = new MolrWebClientImpl(host, port);
    }

    @Override
    public <I> Publisher<MissionEvent> instantiate(ServerInstantiationRequest<I> serverRequest, String
            missionId) {
        return socketClient.instantiate(serverRequest.getMissionName(), missionId, serverRequest.getMissionArguments());
    }

    @Override
    public Publisher<CommandResponse> instruct(MissionCommandRequest commandRequest) {
        return socketClient.instruct("unknown", commandRequest.getMissionId(), commandRequest.getCommand());
    }

    @Override
    public Optional<SupervisorState> getSupervisorState() {
        return client.getState();
    }

}