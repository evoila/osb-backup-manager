package de.evoila.cf.model;

/**
 * Created by yremmet on 27.06.17.
 */
public class BackupRequest {

    private DatabaseCredential source;
    private FileDestination destination;

    public DatabaseCredential getSource() {
        return source;
    }

    public FileDestination getDestination() {
        return destination;
    }

    public void setSource (DatabaseCredential source) {
        this.source = source;
    }

    public void setDestination (FileDestination destination) {
        this.destination = destination;
    }
}
