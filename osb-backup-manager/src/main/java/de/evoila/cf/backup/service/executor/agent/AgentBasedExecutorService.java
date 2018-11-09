package de.evoila.cf.backup.service.executor.agent;


import de.evoila.cf.backup.service.AbstractBackupService;
import de.evoila.cf.model.agent.response.AgentExecutionReponse;
import de.evoila.cf.model.api.endpoint.EndpointCredential;
import de.evoila.cf.model.enums.BackupType;
import de.evoila.cf.model.enums.DestinationType;
import de.evoila.cf.model.enums.JobStatus;
import de.evoila.cf.security.AcceptSelfSignedClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
public class AgentBasedExecutorService extends AbstractBackupService {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected RestTemplate restTemplate;

    protected HttpHeaders headers;

    public List<DestinationType> getDestinationTypes() {
        return this.destinationTypes;
    }

    public BackupType getSourceType() { return BackupType.AGENT; }

    public AgentBasedExecutorService() {
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    private void postConstruct () {
        destinationTypes.add(DestinationType.SWIFT);
        destinationTypes.add(DestinationType.S3);

        this.headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        headers.add("Authorization", "Basic YmFja3VwOnJ1bGV6");
    }

    @ConditionalOnBean(AcceptSelfSignedClientHttpRequestFactory.class)
    @Autowired(required = false)
    private void selfSignedRestTemplate(AcceptSelfSignedClientHttpRequestFactory requestFactory) {
        restTemplate.setRequestFactory(requestFactory);
    }

    public <T extends AgentExecutionReponse> T pollExecutionState(EndpointCredential endpointCredential, String suffix, String id,
                                                                  ParameterizedTypeReference<T> type) {
        T agentExecutionResponse = null;
        try {
            log.info("Polling state of Backup Process for " + id);
            HttpEntity entity = new HttpEntity(headers);
            ResponseEntity<T> agentExecutionResponseEntity = restTemplate
                    .exchange("http://" + endpointCredential.getHost() + ":8000/" + suffix + "/" + id,
                            HttpMethod.GET, entity, type);
            agentExecutionResponse = agentExecutionResponseEntity.getBody();
        } catch (Exception ex) {
            agentExecutionResponse.setStatus(JobStatus.FAILED);
            // we don't need to here anything.
        }
        return agentExecutionResponse;
    }

}
