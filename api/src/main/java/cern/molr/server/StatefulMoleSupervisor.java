package cern.molr.server;

import cern.molr.mole.supervisor.MoleSupervisor;

import java.util.Optional;

/**
 * It represents a supervisor which can tell whether it is idle or not
 *
 * @author yassine
 */
public interface StatefulMoleSupervisor extends MoleSupervisor {
    Optional<State> getState();

    /**
     * Supervisor state
     */
    class State{
        private boolean available;
        private int numMissions;
        private int maxMissions;

        public State() {
        }

        public State(int numMissions, int maxMissions) {
            this.numMissions = numMissions;
            this.maxMissions = maxMissions;
        }

        public boolean isAvailable() {
            return numMissions<maxMissions;
        }

        public int getNumMissions() {
            return numMissions;
        }

        public void setNumMissions(int numMissions) {
            this.numMissions = numMissions;
        }

        public int getMaxMissions() {
            return maxMissions;
        }

        public void setMaxMissions(int maxMissions) {
            this.maxMissions = maxMissions;
        }
    }
}
