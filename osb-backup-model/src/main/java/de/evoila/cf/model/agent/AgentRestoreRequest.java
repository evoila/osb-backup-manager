package de.evoila.cf.model.agent;

import de.evoila.cf.model.api.endpoint.EndpointCredential;
import de.evoila.cf.model.api.file.FileDestination;

/**
 * @author Johannes Hiemer.
 */
public class AgentRestoreRequest extends AbstractRequest {

    private FileDestination destination;

    private EndpointCredential restore;

    public AgentRestoreRequest() {}

    public AgentRestoreRequest(String id, boolean compression, String privateKey,
                               FileDestination destination, EndpointCredential restore) {
        this.id = id;
        this.destination = destination;
        this.restore = restore;
        this.compression = compression;
        this.encryptionKey = privateKey;
    }

    public FileDestination getDestination() {
        return destination;
    }

    public void setDestination(FileDestination destination) {
        this.destination = destination;
    }

    public EndpointCredential getRestore() {
        return restore;
    }

    public void setRestore(EndpointCredential restore) {
        this.restore = restore;
    }
}
