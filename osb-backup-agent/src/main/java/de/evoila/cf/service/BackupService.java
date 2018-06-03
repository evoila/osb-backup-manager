package de.evoila.cf.service;


import de.evoila.cf.controller.exception.BackupException;
import de.evoila.cf.model.BackupJob;
import de.evoila.cf.model.EndpointCredential;
import de.evoila.cf.model.FileDestination;
import de.evoila.cf.model.enums.DatabaseType;
import de.evoila.cf.model.enums.DestinationType;
import de.evoila.cf.openstack.OSException;
import de.evoila.cf.service.exception.ProcessException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by yremmet on 27.06.17.
 */
public interface BackupService {

    DatabaseType getSourceType();

    List<DestinationType> getDestinationTypes();

    Map<String, String> backup(EndpointCredential source, FileDestination destination, BackupJob job) throws IOException,
            InterruptedException, OSException, ProcessException, BackupException;

    void restore(EndpointCredential destination, FileDestination source, BackupJob job) throws IOException,
            OSException, InterruptedException, ProcessException, BackupException;
}
