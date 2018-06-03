package de.evoila.cf.model;

/**
 * Created by yremmet on 27.06.17.
 */
public class BackupRequest {

    private EndpointCredential source;
    private String destinationId;

    public EndpointCredential getSource() {
        return source;
    }

    public void setSource (EndpointCredential source) {
        this.source = source;
    }

    public void setDestinationId (String destinationId) {
        this.destinationId = destinationId;
    }

    public String getDestinationId () {
        return destinationId;
    }
}
