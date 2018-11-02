package de.evoila.cf.backup.service;


import de.evoila.cf.config.security.AcceptSelfSignedClientHttpRequestFactory;
import de.evoila.cf.model.BackupJob;
import de.evoila.cf.model.BackupPlan;
import de.evoila.cf.model.EndpointCredential;
import de.evoila.cf.model.FileDestination;
import de.evoila.cf.model.enums.BackupType;
import de.evoila.cf.model.enums.DestinationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Service
public class AgentBasedBackupService extends AbstractBackupService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private RestTemplate restTemplate;
    private HttpHeaders headers;

    public AgentBasedBackupService() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public BackupType getSourceType() {
        return BackupType.AGENT;
    }

    @Override
    public List<DestinationType> getDestinationTypes() {
        return destinationTypes;
    }

    @PostConstruct
    private void postConstruct () {
        destinationTypes.add(DestinationType.SWIFT);
        destinationTypes.add(DestinationType.S3);

        this.headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
    }

    @ConditionalOnBean(AcceptSelfSignedClientHttpRequestFactory.class)
    @Autowired(required = false)
    private void selfSignedRestTemplate(AcceptSelfSignedClientHttpRequestFactory requestFactory) {
        restTemplate.setRequestFactory(requestFactory);
    }

    @Override
    public Map<String, String> backup(BackupPlan plan, FileDestination destination, BackupJob job) {
        long s_time = System.currentTimeMillis();

        EndpointCredential endpointCredential = plan.getSource();
        Map<String, String> backupFiles = new HashMap<>();
        for (String collection : plan.getItems()) {
            log.info(String.format("Starting backup process to %s:%d/%s",
                    endpointCredential.getHostname(),
                    endpointCredential.getPort(),
                    collection
            ));

            HttpEntity entity = new HttpEntity(plan, headers);

            // TODO WHAT?
            //restTemplate.exchange(backupConfiguration.getUri() + "/plans",
            //        HttpMethod.POST, entity, BackupPlan.class
            //);

            log.info("Calling Backup Run");

            //backup.delete();
            //backupFiles.put(collection, filePath);
        }
        return backupFiles;
    }

    @Override
    public void restore(EndpointCredential destination, FileDestination source, BackupJob job) {
        long s_time = System.currentTimeMillis();
        // TODO: ???? Implement Restore
    }

}
