package de.evoila.cf.backup.service.manager;

import de.evoila.cf.backup.repository.AbstractJobRepository;
import de.evoila.cf.backup.service.CredentialService;
import de.evoila.cf.backup.service.executor.BackupExecutorService;
import de.evoila.cf.backup.service.executor.RestoreExecutorService;
import de.evoila.cf.model.agent.response.AgentExecutionResponse;
import de.evoila.cf.model.api.AbstractJob;
import de.evoila.cf.model.enums.BackupType;
import de.evoila.cf.model.enums.DestinationType;
import de.evoila.cf.model.enums.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
public class AbstractServiceManager {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private List<BackupExecutorService> backupExecutorServices = new ArrayList<>();

    private List<RestoreExecutorService> restoreExecutorServices = new ArrayList<>();

    protected ThreadPoolTaskExecutor taskExecutor;

    protected AbstractJobRepository abstractJobRepository;

    protected CredentialService credentialService;

    @PostConstruct
    private void postConstruct() {
        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(2);
        taskExecutor.setMaxPoolSize(10);
        taskExecutor.initialize();
    }

    protected void updateState(AbstractJob abstractJob, JobStatus status) {
        abstractJob.setStatus(status);
        abstractJobRepository.save(abstractJob);
    }

    protected void updateWithAgentResponse(AbstractJob abstractJob, String item, AgentExecutionResponse agentExecutionResponse) {
        abstractJob.getAgentExecutionReponses().put(item, agentExecutionResponse);
        this.updateState(abstractJob, agentExecutionResponse.getStatus());
    }

    protected void updateStateAndLog(AbstractJob abstractJob, JobStatus status, String log) {
        abstractJob.appendLog(log);
        abstractJob.setStatus(status);
        abstractJobRepository.save(abstractJob);
    }

    protected void addBackupExecutorService(BackupExecutorService backupExecutorService) {
        this.backupExecutorServices.add(backupExecutorService);
    }

    protected void addRestoreExecutorService(RestoreExecutorService restoreExecutorService) {
        this.restoreExecutorServices.add(restoreExecutorService);
    }

    protected List<BackupExecutorService> getBackupExecutorServices() {
        return this.backupExecutorServices;
    }

    protected List<RestoreExecutorService> getRestoreExecutorServices() {
        return this.restoreExecutorServices;
    }

    protected Optional<BackupExecutorService> getApplicableBackupService(BackupType sourceType, DestinationType destType) {
        Optional<BackupExecutorService> service = this.backupExecutorServices
                .stream()
                .filter(s -> s.getSourceType().equals(sourceType))
                .filter(s -> s.getDestinationTypes().contains(destType))
                .findFirst();

        return service;
    }

    protected Optional<RestoreExecutorService> getApplicableRestoreService(BackupType sourceType, DestinationType destType) {
        Optional<RestoreExecutorService> service = this.restoreExecutorServices
                .stream()
                .filter(s -> s.getSourceType().equals(sourceType))
                .filter(s -> s.getDestinationTypes().contains(destType))
                .findFirst();

        return service;
    }

}
