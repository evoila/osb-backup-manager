package de.evoila.cf.backup.service;


import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.model.BackupJob;
import de.evoila.cf.model.BackupPlan;
import de.evoila.cf.model.EndpointCredential;
import de.evoila.cf.model.FileDestination;
import de.evoila.cf.model.enums.BackupType;
import de.evoila.cf.model.enums.DestinationType;
import de.evoila.cf.backup.clients.exception.SwiftClientException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Johannes Hiemer, Yannic Remmet.
 */
public interface BackupService {

    BackupType getSourceType();

    List<DestinationType> getDestinationTypes();

    Map<String, String> backup(BackupPlan plan, FileDestination destination, BackupJob job) throws IOException,
            InterruptedException, SwiftClientException, BackupException;

    void restore(EndpointCredential destination, FileDestination source, BackupJob job) throws IOException,
            SwiftClientException, InterruptedException, BackupException;
}
