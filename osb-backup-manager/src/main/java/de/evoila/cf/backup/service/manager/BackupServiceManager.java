package de.evoila.cf.backup.service.manager;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.repository.AbstractJobRepository;
import de.evoila.cf.backup.service.CredentialService;
import de.evoila.cf.backup.service.exception.BackupRequestException;
import de.evoila.cf.backup.service.executor.BackupExecutorService;
import de.evoila.cf.model.agent.response.AgentBackupResponse;
import de.evoila.cf.model.api.BackupJob;
import de.evoila.cf.model.api.BackupPlan;
import de.evoila.cf.model.api.endpoint.EndpointCredential;
import de.evoila.cf.model.api.file.FileDestination;
import de.evoila.cf.model.api.request.BackupRequest;
import de.evoila.cf.model.enums.JobStatus;
import de.evoila.cf.model.enums.JobType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Component
public class BackupServiceManager extends AbstractServiceManager {

    public BackupServiceManager(AbstractJobRepository abstractJobRepository,
                                CredentialService credentialService,
                                List<BackupExecutorService> backupExecutorServices) {
        for (BackupExecutorService backupExecutorService : backupExecutorServices)
            this.addBackupExecutorService(backupExecutorService);
        this.abstractJobRepository = abstractJobRepository;
        this.credentialService = credentialService;
    }


    public BackupJob backup(BackupRequest backupRequest) throws BackupRequestException, BackupException {
        if (backupRequest.getBackupPlan() == null || backupRequest.getBackupPlan().getFileDestination() == null)
            throw new BackupException("Did not find backup plan or destination");

        return backup(backupRequest.getBackupPlan(), backupRequest.getBackupPlan().getFileDestination());
    }

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

    private void executeBackup(BackupExecutorService backupExecutorService, EndpointCredential endpointCredential, BackupJob backupJob,
                               FileDestination destination, List<String> items) {
        try {
            log.info("Starting execution of Backup Job");
            updateState(backupJob, JobStatus.RUNNING);

            int i = 0;
            for (String item : items) {
                String id = backupJob.getIdAsString() + i;

                BackupPlan backupPlan = backupJob.getBackupPlan();
                backupExecutorService.backup(endpointCredential, destination, id, item,
                        backupPlan.isCompression(), backupPlan.getPublicKey());
                backupJob.setDestination(destination);

                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                CompletableFuture<AgentBackupResponse> completionFuture = new CompletableFuture<>();
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

        } catch (Exception e) {
            log.error("Exception during backup execution", e);
            updateStateAndLog(backupJob, JobStatus.FAILED, String.format("An error occurred (%s) : %s", backupJob.getId(), e.getMessage()));
        }
    }
}
