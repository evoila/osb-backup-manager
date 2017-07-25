package de.evoila.cf.service;

import de.evoila.cf.model.*;
import de.evoila.cf.model.enums.DatabaseType;
import de.evoila.cf.model.enums.DestinationType;
import de.evoila.cf.openstack.OSException;
import de.evoila.cf.service.exception.ProcessException;

import java.io.IOException;
import java.util.List;

/**
 * Created by yremmet on 27.06.17.
 */
public interface BackupService {
    public DatabaseType getSourceType();
    public List<DestinationType> getDestinationTypes();
    public String backup(DatabaseCredential source, FileDestination destination, BackupJob job) throws IOException, InterruptedException, OSException, ProcessException;
    void restore(DatabaseCredential destination, FileDestination source, BackupJob job) throws IOException, OSException, InterruptedException, ProcessException;
}