package de.evoila.cf.backup.service.manager;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.repository.AbstractJobRepository;
import de.evoila.cf.backup.service.BackupCleanupManager;
import de.evoila.cf.backup.service.CredentialService;
import de.evoila.cf.backup.service.exception.BackupRequestException;
import de.evoila.cf.backup.service.executor.BackupExecutorService;
import de.evoila.cf.model.agent.response.AgentBackupResponse;
import de.evoila.cf.model.api.AbstractJob;
import de.evoila.cf.model.api.BackupJob;
import de.evoila.cf.model.api.BackupPlan;
import de.evoila.cf.model.api.endpoint.EndpointCredential;
import de.evoila.cf.model.api.file.FileDestination;
import de.evoila.cf.model.api.request.BackupRequest;
import de.evoila.cf.model.enums.JobStatus;
import de.evoila.cf.model.enums.JobType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 *
 * The BackupServiceManager provides methods for executing backups with an implemented ExecutorService. BackupRequests
 * are used to trigger the backup process and a BackupJob will be added to the repository. During the process, the
 * status of the BackupJob will be updated by communicating with the ExecuterService.
 */
@Component
public class BackupServiceManager extends AbstractServiceManager {

    BackupCleanupManager backupCleanupManager;

    public BackupServiceManager(AbstractJobRepository abstractJobRepository,
                                CredentialService credentialService,
                                List<BackupExecutorService> backupExecutorServices,
                                BackupCleanupManager backupCleanupManager) {
        for (BackupExecutorService backupExecutorService : backupExecutorServices)
            this.addBackupExecutorService(backupExecutorService);
        this.abstractJobRepository = abstractJobRepository;
        this.credentialService = credentialService;
        this.backupCleanupManager = backupCleanupManager;
    }

    /**
     * Execute a backup with the information provided in the BackupRequest. This method will fetch the BackupPlan
     * and FileDestination provided with the BackupRequest for further processing.
     *
     * @param backupRequest Information needed for executing the backup
     * @return The created BackupJob
     * @throws BackupRequestException
     * @throws BackupException
     */
    public BackupJob backup(BackupRequest backupRequest) throws BackupRequestException, BackupException {
        if (backupRequest.getBackupPlan() == null || backupRequest.getBackupPlan().getFileDestination() == null)
            throw new BackupException("Did not find backup plan or destination");

        return backup(backupRequest.getBackupPlan(), backupRequest.getBackupPlan().getFileDestination());
    }

    /**
     * Execute a backup with the information provided in the BackupPlan and FileDestination. Creates a new
     * BackupJob in the repository, fetches the credentials to access the service instance and looks up the
     * necessary BackupExecutorService needed to execute the backup. When all of these operations have been
     * successful, the backup will be executed.
     *
     * @param backupPlan A BackupPlan
     * @param destination A FileDestination for the backup file to be stored
     * @return The created BackupJob
     * @throws BackupRequestException
     */
    public BackupJob backup(BackupPlan backupPlan, FileDestination destination) throws BackupRequestException {
        BackupJob backupJob = new BackupJob(JobType.BACKUP, backupPlan.getServiceInstance(), JobStatus.STARTED);
        backupJob.setBackupPlan(backupPlan);
        abstractJobRepository.save(backupJob);

        EndpointCredential endpointCredential;
        try {
            endpointCredential = credentialService.getCredentials(backupPlan.getServiceInstance());
        } catch (BackupException ex) {
            throw new BackupRequestException("Could not load endpoint credentials", ex);
        }

        if (endpointCredential == null)
            throw new BackupRequestException("Did not find Service Instance");

        Optional<BackupExecutorService> backupExecutorService = this
                .getApplicableBackupService(endpointCredential.getType(), destination.getType());

        if (!backupExecutorService.isPresent()) {
            String msg = String.format("No Backup Service found (JOB=%s) for Database %s",
                    backupJob.getId(),
                    endpointCredential.getType())
                    + getBackupExecutorServices()
                    .stream()
                    .map(s -> s.getSourceType().toString())
                    .collect(Collectors.toList());
            log.warn(msg);

            updateStateAndLog(backupJob, JobStatus.FAILED, msg);

            throw new BackupRequestException("No Backup Service found");
        }

        taskExecutor.execute(() -> executeBackup(backupExecutorService.get(), endpointCredential,
                backupJob, destination, backupPlan.getItems()));

        return backupJob;
    }

    /**
     * Execute a BackupJob. During the process, the JobStatus of the BackupJob will be continuously updated. If a
     * backup has been successfully executed, then old backup files which are exceeding the retention period will be
     * automatically removed too.
     *
     * @param backupExecutorService Service with a connection to a component which can create backup files
     * @param endpointCredential Credentials for the FileDestination
     * @param backupJob A BackupJob
     * @param destination Location to store the backup files
     * @param items the storage systems (e.g. database) from the service instance to be backed up
     */
    private void executeBackup(BackupExecutorService backupExecutorService, EndpointCredential endpointCredential, BackupJob backupJob,
                               FileDestination destination, List<String> items) {
        try {
            log.info("Starting execution of Backup Job");
            updateState(backupJob, JobStatus.RUNNING);

            List<CompletableFuture<AgentBackupResponse>> completionFutures = new ArrayList<>();

            int i = 0;
            for (String item : items) {
                String id = backupJob.getIdAsString() + i;

                BackupPlan backupPlan = backupJob.getBackupPlan();
                backupExecutorService.backup(endpointCredential, destination, id, item,
                        backupPlan.isCompression(), backupPlan.getPublicKey(), backupPlan.getIdAsString());
                backupJob.setDestination(destination);

                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                CompletableFuture<AgentBackupResponse> completionFuture = new CompletableFuture<>();
                completionFutures.add(completionFuture);
                ScheduledFuture checkFuture = executor.scheduleAtFixedRate(() -> {
                    try {
                        AgentBackupResponse agentBackupResponse = backupExecutorService.pollExecutionState(endpointCredential,
                                "backup", id, new ParameterizedTypeReference<AgentBackupResponse>() {});

                        updateWithAgentResponse(backupJob, item, agentBackupResponse);
                        if (!agentBackupResponse.getStatus().equals(JobStatus.RUNNING)) {
                            completionFuture.complete(agentBackupResponse);
                        }
                    } catch (BackupException ex) {
                        completionFuture.complete(null);
                    }

                }, 0, 5, TimeUnit.SECONDS);
                completionFuture.whenComplete((result, thrown) -> {
                    if (result != null) {
                        if (result.getStatus().equals(JobStatus.SUCCEEDED)) {
                            backupJob.getFiles().put(item, result.getFilename());
                        }

                        updateWithAgentResponse(backupJob, item, result);
                    }

                    checkFuture.cancel(true);
                    log.info("Finished execution of Backup Job");
                });
                i++;
            }

            for(CompletableFuture completionFuture : completionFutures) {
                completionFuture.get();
            }

            if(!backupJob.getStatus().equals(JobStatus.FAILED)) {
                deleteIfDataRetentionIsReached(backupJob.getBackupPlan());
            }

        } catch (Exception e) {
            log.error("Exception during backup execution", e);
            updateStateAndLog(backupJob, JobStatus.FAILED, String.format("An error occurred (%s) : %s", backupJob.getId(), e.getMessage()));
        }
    }

    /**
     * Delete old BackupJob and backup files which exceed the configured retention period.
     *
     * @param backupPlan A BackupPlan
     */
    public void deleteIfDataRetentionIsReached(BackupPlan backupPlan) {
        List<AbstractJob> jobs = abstractJobRepository.findByBackupPlan(backupPlan);
        List<BackupJob> backupJobs = new ArrayList<>();

        for(AbstractJob job : jobs) {
            if(job.getJobType().equals(JobType.BACKUP) && job.getStatus().equals(JobStatus.SUCCEEDED)) {
                backupJobs.add((BackupJob) job);
            }
        }

        if(backupJobs.size() > backupPlan.getRetentionPeriod()) {
            Collections.sort(backupJobs, Comparator.comparing(AbstractJob::getStartDate));

            log.info("Retention limit of " + backupPlan.getRetentionPeriod() + " reached");
            log.info("Deleting oldest backup files related to plan " + backupPlan.getName());

            while(backupJobs.size() > backupPlan.getRetentionPeriod()) {
                backupCleanupManager.delete(backupJobs.remove(0), backupPlan.getFileDestination());
            }
        }
    }
}
