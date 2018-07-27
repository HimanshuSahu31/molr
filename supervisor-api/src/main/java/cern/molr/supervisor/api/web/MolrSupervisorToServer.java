package cern.molr.supervisor.api.web;

import java.util.List;

/**
 * A client service which allows to perform some defined requests to a the MolR server
 *
 * @author yassine-kr
 */
public interface MolrSupervisorToServer {

    /**
     * It is a synchronous method which sends a registration request and waits for the response
     * It should throw an unchecked exception if there was a connection error or the response was a failure
     *
     * @return the supervisor Id generated by the server
     */
    String register(String host, int port, List<String> acceptedMissions);

    /**
     * It is a synchronous method which sends a registration request and waits for the response
     * It should throw an unchecked exception if there was a connection error or the response was a failure
     */
    void unregister(String supervisorId);
}
