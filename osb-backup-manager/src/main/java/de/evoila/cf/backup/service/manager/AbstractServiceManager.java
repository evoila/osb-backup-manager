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

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
public class AbstractServiceManager {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private List<BackupExecutorService> backupExecutorServices = new ArrayList<>();

    private List<RestoreExecutorService> restoreExecutorServices = new ArrayList<>();

    protected ThreadPoolTaskExecutor taskExecutor;

    protected ScheduledExecutorService scheduledExcecutor;

    protected AbstractJobRepository abstractJobRepository;

    protected CredentialService credentialService;

    @PostConstruct
    private void postConstruct() {
        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(5);
        taskExecutor.setMaxPoolSize(4096);
        taskExecutor.setQueueCapacity(50);
        taskExecutor.initialize();

        scheduledExcecutor = Executors.newScheduledThreadPool(1);
    }

    /**
     * Update the JobStatus of a job and save it in the repository.
     *
     * @param abstractJob a job to be updated
     * @param status the new status of a job
     */
    protected void updateState(AbstractJob abstractJob, JobStatus status) {
        abstractJob.setStatus(status);
        abstractJobRepository.save(abstractJob);
    }

    /**
     * Update the status of a job and the specific status on the database.
     *
     * @param abstractJob a job to be updated
     * @param item a database in the destination
     * @param agentExecutionResponse the response from an agent for the progress on the database
     */
    protected void updateWithAgentResponse(AbstractJob abstractJob, String item, AgentExecutionResponse agentExecutionResponse) {
        abstractJob.getAgentExecutionReponses().put(item, agentExecutionResponse);

        JobStatus status = abstractJob.getAgentExecutionReponses()
                .values()
                .stream()
                .map(AgentExecutionResponse::getStatus)
                .max(Comparator.comparingInt(jobStatus -> {
                    int priority = 10;
                    switch (jobStatus) {
                        case UNKNOWN:
                            priority = 5;
                            break;
                        case RUNNING:
                            priority = 4;
                            break;
                        case STARTED:
                            priority= 3;
                            break;
                        case FAILED:
                            priority = 2;
                            break;
                        case SUCCEEDED:
                            priority = 1;
                            break;
                    }
                    return priority;
                }))
                .orElse(JobStatus.UNKNOWN);
        updateState(abstractJob, status);
    }

    /**
     * Update the JobStatus of a job, save it in a repository and write a message in the log of the job.
     *
     * @param abstractJob a job to be updated
     * @param status the new status of the job
     * @param log a log message to be written in the log of the job
     */
    protected void updateStateAndLog(AbstractJob abstractJob, JobStatus status, String log) {
        abstractJob.appendLog(log);
        abstractJob.setStatus(status);
        abstractJobRepository.save(abstractJob);
    }

    /**
     * Add a BackupExecutorService to a list of available executors.
     *
     * @param backupExecutorService a new BackupExecutorService to be added
     */
    protected void addBackupExecutorService(BackupExecutorService backupExecutorService) {
        this.backupExecutorServices.add(backupExecutorService);
    }

    /**
     * Add a RestoreExecutorService to a list of available executors.
     *
     * @param restoreExecutorService a new RestoreExecutorService to be added
     */
    protected void addRestoreExecutorService(RestoreExecutorService restoreExecutorService) {
        this.restoreExecutorServices.add(restoreExecutorService);
    }

    /**
     * Get all added BackupExecutorServices.
     *
     * @return a list of BackupExecutorService
     */
    protected List<BackupExecutorService> getBackupExecutorServices() {
        return this.backupExecutorServices;
    }

    /**
     * Get all added RestoreExecutorServices.
     *
     * @return a list of RestoreExecutorService
     */
    protected List<RestoreExecutorService> getRestoreExecutorServices() {
        return this.restoreExecutorServices;
    }

    /**
     * Get an applicable BackupExecutorService, which uses the BackupType and DestinationType.
     *
     * @param sourceType the type of BackupExecutorService
     * @param destType the type of the storage
     * @return an Optional with the response
     */
    protected Optional<BackupExecutorService> getApplicableBackupService(BackupType sourceType, DestinationType destType) {
        Optional<BackupExecutorService> service = this.backupExecutorServices
                .stream()
                .filter(s -> s.getSourceType().equals(sourceType))
                .filter(s -> s.getDestinationTypes().contains(destType))
                .findFirst();

        return service;
    }

    /**
     * Get an applicable RestoreExecutorService, which uses the BackupType and DestinationType.
     *
     * @param sourceType the type of RestoreExecutorService
     * @param destType the type of the storage
     * @return an Optional with the response
     */
    protected Optional<RestoreExecutorService> getApplicableRestoreService(BackupType sourceType, DestinationType destType) {
        Optional<RestoreExecutorService> service = this.restoreExecutorServices
                .stream()
                .filter(s -> s.getSourceType().equals(sourceType))
                .filter(s -> s.getDestinationTypes().contains(destType))
                .findFirst();

        return service;
    }

}
