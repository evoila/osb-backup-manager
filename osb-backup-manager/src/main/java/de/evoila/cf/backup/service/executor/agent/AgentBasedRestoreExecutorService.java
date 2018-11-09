package de.evoila.cf.backup.service.executor.agent;


import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.service.executor.RestoreExecutorService;
import de.evoila.cf.model.agent.AgentRestoreRequest;
import de.evoila.cf.model.agent.response.AgentRestoreResponse;
import de.evoila.cf.model.api.endpoint.EndpointCredential;
import de.evoila.cf.model.api.file.FileDestination;
import de.evoila.cf.model.api.request.RequestDetails;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Service
public class AgentBasedRestoreExecutorService extends AgentBasedExecutorService implements RestoreExecutorService {

    @Override
    public void restore(EndpointCredential endpointCredential, FileDestination destination,
                        RequestDetails requestDetails, String id) throws BackupException {
        endpointCredential.setDatabase(requestDetails.getItem());
        destination.setFilename(requestDetails.getFilename());

        log.info(String.format("Starting restore process to %s:%d/%s",
                endpointCredential.getHost(),
                endpointCredential.getPort(),
                requestDetails.getItem()
        ));

        log.info("Calling Agent to run Restore Process");
        AgentRestoreRequest agentRestoreRequest = new AgentRestoreRequest(id,
                destination, endpointCredential);
        headers.add("Authorization", "Basic YmFja3VwOnJ1bGV6");
        HttpEntity entity = new HttpEntity(agentRestoreRequest, headers);

        try {
            ResponseEntity<AgentRestoreResponse> agentRestoreResponse = restTemplate
                .exchange("http://" + endpointCredential.getHost() + ":8000/restore",
                        HttpMethod.PUT, entity, AgentRestoreResponse.class);
        } catch (Exception ex) {
            throw new BackupException("Error during Backup Process Run Call", ex);
            // we don't need to here anything.
        }
    }

}
