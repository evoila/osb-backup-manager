package de.evoila.cf.backup.service.executor.agent;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.service.executor.BackupExecutorService;
import de.evoila.cf.model.agent.AgentBackupRequest;
import de.evoila.cf.model.agent.response.AgentBackupResponse;
import de.evoila.cf.model.api.endpoint.EndpointCredential;
import de.evoila.cf.model.api.file.FileDestination;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Service
public class AgentBasedBackupExecutorService extends AgentBasedExecutorService implements BackupExecutorService {

    public AgentBasedBackupExecutorService() {}

    @Override
    public void backup(EndpointCredential endpointCredential, FileDestination destination, String id,
                       String item, boolean compression, String publicKey) throws BackupException {
        endpointCredential.setDatabase(item);

        log.info(String.format("Starting backup process to %s:%d/%s",
                endpointCredential.getHost(),
                endpointCredential.getPort(),
                item
        ));

        log.info("Calling Agent to run Backup Process");
        AgentBackupRequest agentBackupRequest = new AgentBackupRequest(id, compression, publicKey,
                destination, endpointCredential);

        log.info(String.format("Credentials are %s:%s",
                endpointCredential.getBackupUsername(),
                endpointCredential.getBackupPassword()
        ));
        this.setAuthenticationHeader(endpointCredential.getBackupUsername(),
                endpointCredential.getBackupPassword());
        HttpEntity entity = new HttpEntity(agentBackupRequest, headers);

        try {
            restTemplate
                    .exchange("http://" + endpointCredential.getHost() + ":8000/backup",
                            HttpMethod.POST, entity, AgentBackupResponse.class);
        } catch (Exception ex) {
            throw new BackupException("Error during Backup Process Run Call", ex);
        }

    }

}
