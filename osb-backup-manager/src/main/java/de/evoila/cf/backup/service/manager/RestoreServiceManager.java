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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 *
 * The BackupServiceManager provides methods for restoring backups with an implemented ExecutorService. RestoreRequests
 * are used to trigger the restoration process and a RestoreJob will be added to the repository. During the process, the
 * status of the RestoreJob will be updated by communicating with the ExecuterService.
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

    /**
     * Restore a backup with the information provided in the RestoreRequest. This method checks if a FileDestination
     * has been configured, the service instance is available and credentials to access the service instance exist.
     * When all of these operations have been successful, the backup will be executed.
     *
     * @param restoreRequest A RestoreRequest to be executed
     * @return The created RestoreJob
     * @throws BackupRequestException
     */
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

    /**
     * Restore a backup with the information provided. Create a RestoreJob and save it in the repository.
     * Looks up the appropriate ExecutorService for the job. Continually updates the JobStatus during the execution.
     *
     * @param backupPlan A BackupPlan
     * @param endpointCredential Credentials for accessing the service instance
     * @param destination FileDestination with the stored backup file
     * @param items List of backup files to be restored
     * @return The created RestoreJob
     * @throws BackupRequestException
     */
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

    /**
     * Execute a RestoreJob. During the process, the JobStatus of the RestoreJob will be continuously updated. Backup
     * files stored in the specified destination and database instances will be restored.
     *
     * @param restoreExecutorService Service with a connection to the component, which can restore backup files
     * @param restoreJob A RestoreJob
     * @param endpointCredential Credentials for the ServiceInstance
     * @param destination Location to restore the backup files from
     * @param items Database instances on the specified destination
     */
    private void executeRestore(RestoreExecutorService restoreExecutorService, RestoreJob restoreJob, EndpointCredential endpointCredential,
                                FileDestination destination, List<RequestDetails> items) {
        try {
            log.info("Starting execution of Restore Job");
            updateState(restoreJob, JobStatus.RUNNING);

            List<CompletableFuture<AgentRestoreResponse>> completionFutures = new ArrayList<>();

            int i = 0;
            for (RequestDetails requestDetails : items) {
                String id = restoreJob.getIdAsString() + i;

                BackupPlan backupPlan = restoreJob.getBackupPlan();
                restoreExecutorService.restore(endpointCredential, destination, requestDetails, id,
                        backupPlan.isCompression(), backupPlan.getPrivateKey(), backupPlan.getIdAsString());

                CompletableFuture<AgentRestoreResponse> completionFuture = new CompletableFuture<>();
                AtomicInteger persistFailures = new AtomicInteger(0);
                ScheduledFuture<?> checkFuture = scheduledExcecutor.scheduleAtFixedRate(() -> {
                    AgentRestoreResponse agentRestoreResponse;
                    try {
                        agentRestoreResponse = restoreExecutorService.pollExecutionState(endpointCredential,
                                "restore", id, new ParameterizedTypeReference<AgentRestoreResponse>() {});
                    } catch (BackupException ex) {
                        log.error("restore poll failed, creating dummy response", ex);
                        AgentRestoreResponse dummyResponse = new AgentRestoreResponse();
                        dummyResponse.setStatus(JobStatus.FAILED);
                        dummyResponse.setErrorMessage(ex.getMessage());
                        completionFuture.complete(dummyResponse);
                        return;
                    } catch (Exception ex) {
                        log.error("restore poll failed unexpectedly, creating dummy response", ex);
                        AgentRestoreResponse dummyResponse = new AgentRestoreResponse();
                        dummyResponse.setStatus(JobStatus.UNKNOWN);
                        dummyResponse.setErrorMessage(ex.getMessage());
                        completionFuture.complete(dummyResponse);
                        return;
                    }

                    if (agentRestoreResponse == null) {
                        return;
                    }

                    try {
                        updateWithAgentResponse(restoreJob, requestDetails.getItem(), agentRestoreResponse);
                        persistFailures.set(0);
                    } catch (Exception ex) {
                        int failures = persistFailures.incrementAndGet();
                        log.warn("Persisting agent response failed (attempt {} of {}); keeping agent status {} in memory",
                                failures, MAX_CONSECUTIVE_PERSIST_FAILURES, agentRestoreResponse.getStatus(), ex);
                        if (failures >= MAX_CONSECUTIVE_PERSIST_FAILURES) {
                            log.error("Giving up on persistence after {} consecutive failures for restore job {}; " +
                                            "propagating last agent status {} (restore may have completed on the agent)",
                                    failures, restoreJob.getId(), agentRestoreResponse.getStatus(), ex);
                            // Preserve the agent's actual outcome instead of overwriting with FAILED.
                            // For non-terminal agent states we genuinely don't know the outcome anymore,
                            // so report UNKNOWN rather than fabricate SUCCEEDED or FAILED.
                            AgentRestoreResponse finalResponse = agentRestoreResponse;
                            JobStatus agentStatus = agentRestoreResponse.getStatus();
                            if (agentStatus == JobStatus.RUNNING || agentStatus == JobStatus.STARTED) {
                                AgentRestoreResponse unknownResponse = new AgentRestoreResponse();
                                unknownResponse.setStatus(JobStatus.UNKNOWN);
                                unknownResponse.setErrorMessage("persistence unavailable after " + failures
                                        + " attempts; final agent outcome unknown");
                                finalResponse = unknownResponse;
                            }
                            completionFuture.complete(finalResponse);
                            return;
                        }
                    }

                    if (!agentRestoreResponse.getStatus().equals(JobStatus.RUNNING)) {
                        completionFuture.complete(agentRestoreResponse);
                    }
                }, 0, 5, TimeUnit.SECONDS);

                completionFutures.add(completionFuture.whenComplete((result, thrown) -> {
                    if (result != null) {
                        // Best-effort final persistence. If it fails we must NOT let the exception
                        // escape — otherwise the outer catch would overwrite an authentic SUCCEEDED
                        // status (preserved by the polling failsafe) with FAILED, undoing the truth.
                        try {
                            updateWithAgentResponse(restoreJob, requestDetails.getItem(), result);
                        } catch (Exception persistEx) {
                            log.warn("Final persistence of restore job {} failed; agent reported {} " +
                                            "and in-memory state reflects that, but MongoDB may be stale",
                                    restoreJob.getId(), result.getStatus(), persistEx);
                        }
                    }

                    checkFuture.cancel(true);
                    log.info("Finished execution of Restore Job");
                }));
                i++;
            }
            for(CompletableFuture<?> completionFuture : completionFutures) {
                completionFuture.get();
            }
        } catch (BackupException | InterruptedException | ExecutionException e) {
            log.error("Exception during restore execution", e);
            // Don't clobber a SUCCEEDED status that the polling failsafe already preserved.
            // Try one more save with SUCCEEDED so that a transient MongoDB recovery between
            // whenComplete and here still gets the truth persisted.
            if (restoreJob.getStatus() != JobStatus.SUCCEEDED) {
                updateStateAndLog(restoreJob, JobStatus.FAILED, String.format("An error occurred (%s) : %s", restoreJob.getId(), e.getMessage()));
            } else {
                log.warn("Restore job {} reported SUCCEEDED by the agent but final orchestration threw; " +
                        "attempting to persist SUCCEEDED before exiting", restoreJob.getId());
                try {
                    updateStateAndLog(restoreJob, JobStatus.SUCCEEDED, String.format(
                            "Agent reported SUCCEEDED; final orchestration threw (%s) : %s",
                            restoreJob.getId(), e.getMessage()));
                } catch (Exception persistEx) {
                    log.warn("Final SUCCEEDED save for restore job {} also failed; in-memory state is correct but MongoDB may be stale",
                            restoreJob.getId(), persistEx);
                }
            }
            if(e instanceof InterruptedException){
                Thread.currentThread().interrupt();
            }
        }
    }
}
