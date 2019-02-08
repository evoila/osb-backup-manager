package de.evoila.cf.backup.service.manager;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.repository.AbstractJobRepository;
import de.evoila.cf.backup.service.CredentialService;
import de.evoila.cf.backup.service.exception.BackupRequestException;
import de.evoila.cf.backup.service.executor.RestoreExecutorService;
import de.evoila.cf.model.agent.response.AgentRestoreResponse;
import de.evoila.cf.model.api.BackupJob;
import de.evoila.cf.model.api.BackupPlan;
import de.evoila.cf.model.api.RestoreJob;
import de.evoila.cf.model.api.endpoint.EndpointCredential;
import de.evoila.cf.model.api.file.FileDestination;
import de.evoila.cf.model.api.request.RequestDetails;
import de.evoila.cf.model.api.request.RestoreRequest;
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
public class RestoreServiceManager extends AbstractServiceManager {

    public RestoreServiceManager(AbstractJobRepository abstractJobRepository,
                                  CredentialService credentialService,
                                 List<RestoreExecutorService> restoreExecutorServices) {
        for (RestoreExecutorService restoreExecutorService : restoreExecutorServices)
            this.addRestoreExecutorService(restoreExecutorService);
        this.abstractJobRepository = abstractJobRepository;
        this.credentialService = credentialService;
    }

    public RestoreJob restore(RestoreRequest restoreRequest) throws BackupRequestException {
        if (restoreRequest.getBackupJob() == null)
            throw new BackupRequestException("Could not find backup job");

        BackupJob backupJob = restoreRequest.getBackupJob();

        if (backupJob.getDestination() == null)
            throw new BackupRequestException("Did not find destination");

        if (backupJob.getServiceInstance() == null)
            throw new BackupRequestException("Did not find Service Instance");

        EndpointCredential endpointCredential;
        try {
            endpointCredential = credentialService.getCredentials(backupJob.getServiceInstance());
        } catch (BackupException ex) {
            throw new BackupRequestException("Could not load endpoint credentials", ex);
        }

        if (endpointCredential == null)
            throw new BackupRequestException("Did not find Service Instance");

        return restore(backupJob.getBackupPlan(), endpointCredential, backupJob.getDestination(), restoreRequest.getItems());
    }

    public RestoreJob restore(BackupPlan backupPlan, EndpointCredential endpointCredential, FileDestination destination,
                              List<RequestDetails> items) throws BackupRequestException {
        RestoreJob restoreJob = new RestoreJob(JobType.RESTORE, endpointCredential.getServiceInstance(), JobStatus.STARTED);
        restoreJob.setBackupPlan(backupPlan);
        abstractJobRepository.save(restoreJob);

        Optional<RestoreExecutorService> restoreExecutorService = this
                .getApplicableRestoreService(endpointCredential.getType(), destination.getType());

        if (!restoreExecutorService.isPresent()) {
            String msg = String.format("No Restore Service found (JOB=%s) for Database %s", restoreJob.getId(), endpointCredential.getType())
                    + getRestoreExecutorServices().stream().map(s -> s.getSourceType().toString()).collect(Collectors.toList());
            log.warn(msg);

            updateStateAndLog(restoreJob, JobStatus.FAILED, msg);
            throw new BackupRequestException("No Restore Service found");
        }

        taskExecutor.execute(() -> {
            executeRestore(restoreExecutorService.get(), restoreJob, endpointCredential, destination, items);
        });
        return restoreJob;
    }

    private void executeRestore(RestoreExecutorService restoreExecutorService, RestoreJob restoreJob, EndpointCredential endpointCredential,
                                FileDestination destination, List<RequestDetails> items) {
        try {
            log.info("Starting execution of Restore Job");
            updateState(restoreJob, JobStatus.RUNNING);

            int i = 0;
            for (RequestDetails requestDetails : items) {
                String id = restoreJob.getIdAsString() + i;

                BackupPlan backupPlan = restoreJob.getBackupPlan();
                restoreExecutorService.restore(endpointCredential, destination, requestDetails, id,
                        backupPlan.isCompression(), backupPlan.getPrivateKey());

                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                CompletableFuture<AgentRestoreResponse> completionFuture = new CompletableFuture<>();
                ScheduledFuture checkFuture = executor.scheduleAtFixedRate(() -> {
                    try {
                        AgentRestoreResponse agentRestoreResponse = restoreExecutorService.pollExecutionState(endpointCredential,
                                "restore", id, new ParameterizedTypeReference<AgentRestoreResponse>() {});

                        updateWithAgentResponse(restoreJob, requestDetails.getItem(), agentRestoreResponse);
                        if (!agentRestoreResponse.getStatus().equals(JobStatus.RUNNING)) {
                            completionFuture.complete((AgentRestoreResponse) agentRestoreResponse);
                        }
                    } catch (BackupException ex) {
                        completionFuture.complete(null);
                    }

                }, 0, 5, TimeUnit.SECONDS);
                completionFuture.whenComplete((result, thrown) -> {
                    if (result != null) {
                        updateWithAgentResponse(restoreJob, requestDetails.getItem(), result);
                    }

                    checkFuture.cancel(true);
                    log.info("Finished execution of Restore Job");
                });
                i++;
            }
        } catch (BackupException e) {
            log.error("Exception during restore execution", e);
            updateStateAndLog(restoreJob, JobStatus.FAILED, String.format("An error occurred (%s) : %s", restoreJob.getId(), e.getMessage()));
        }
    }
}
