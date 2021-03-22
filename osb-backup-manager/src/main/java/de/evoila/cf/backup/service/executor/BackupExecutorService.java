package de.evoila.cf.backup.service.executor;


import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.model.api.endpoint.EndpointCredential;
import de.evoila.cf.model.api.file.FileDestination;

/**
 * @author Johannes Hiemer, Yannic Remmet.
 */
public interface BackupExecutorService extends BaseExecutorService {

    void backup(EndpointCredential endpointCredential, FileDestination destination, String id,
                String item, boolean compression, String encryptionKey, String planId) throws BackupException;

}
