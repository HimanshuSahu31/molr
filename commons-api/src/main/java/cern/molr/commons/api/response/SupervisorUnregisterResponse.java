package cern.molr.commons.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response to an unregistration request, sent back by the MolR server to the supervisor
 *
 * @author yassine-kr
 */
public final class SupervisorUnregisterResponse extends Response<Ack> {

    public SupervisorUnregisterResponse(@JsonProperty("result") Ack result, @JsonProperty("throwable") Throwable
            throwable, @JsonProperty("success") boolean success) {
        super(result, throwable, success);
    }

    public SupervisorUnregisterResponse(Ack result) {
        super(result);
    }

    public SupervisorUnregisterResponse(Throwable throwable) {
        super(throwable);
    }
}
