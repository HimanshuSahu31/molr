package cern.molr.server.request.supervisor;

/**
 * A request sent by a supervisor to unregister itself from MolR server
 *
 * @author yassine
 */
public class SupervisorUnregisterRequest {

    private String id;

    public SupervisorUnregisterRequest() {
    }

    public SupervisorUnregisterRequest(String id) {
        this.id = id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}