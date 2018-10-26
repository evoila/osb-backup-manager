package de.evoila.cf.backup.service;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.model.enums.BackupType;
import de.evoila.cf.model.enums.DestinationType;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yannic Remmet, Johannes Hiemer
 */
public abstract class AbstractBackupService implements BackupService {

    protected static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm");

    protected List<DestinationType> destinationTypes = new ArrayList<DestinationType>();

    private BackupServiceManager serviceManager;

    public List<DestinationType> getDestinationTypes() {
        return this.destinationTypes;
    }

    public BackupType getSourceType() { return BackupType.AGENT; }

    public BackupServiceManager getServiceManager() {
        return serviceManager;
    }

    @Autowired
    public void setServiceManager(BackupServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.serviceManager.addBackupServiceManager(this);
    }

    protected String getBinary(String path) throws BackupException {
        URL toolUrl = this.getClass().getResource(path);
        if (toolUrl == null)
            throw new BackupException("Could not load " + path);
        return toolUrl.getPath();
    }

}