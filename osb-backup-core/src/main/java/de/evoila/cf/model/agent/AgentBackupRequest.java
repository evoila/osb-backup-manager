package de.evoila.cf.model.agent;

import de.evoila.cf.model.api.endpoint.EndpointCredential;
import de.evoila.cf.model.api.file.FileDestination;

/**
 * @author Johannes Hiemer.
 */
public class AgentBackupRequest extends AbstractRequest {

    private FileDestination destination;

    private EndpointCredential backup;

    public AgentBackupRequest() {}

    public AgentBackupRequest(String id, FileDestination destination, EndpointCredential backup) {
        this.id = id;
        this.destination = destination;
        this.backup = backup;
    }

    public FileDestination getDestination() {
        return destination;
    }

    public void setDestination(FileDestination destination) {
        this.destination = destination;
    }

    public EndpointCredential getBackup() {
        return backup;
    }

    public void setBackup(EndpointCredential backup) {
        this.backup = backup;
    }
}
