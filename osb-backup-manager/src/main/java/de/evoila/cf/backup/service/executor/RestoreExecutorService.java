package de.evoila.cf.backup.service.executor;


import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.model.api.endpoint.EndpointCredential;
import de.evoila.cf.model.api.file.FileDestination;
import de.evoila.cf.model.api.request.RequestDetails;

/**
 * @author Johannes Hiemer, Yannic Remmet.
 */
public interface RestoreExecutorService extends BaseExecutorService {

    void restore(EndpointCredential endpointCredential, FileDestination destination,
                 RequestDetails requestDetails, String id) throws BackupException;

}
