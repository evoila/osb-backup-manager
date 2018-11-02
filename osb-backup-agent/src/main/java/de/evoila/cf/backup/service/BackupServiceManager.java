package de.evoila.cf.backup.service;

import de.evoila.cf.backup.clients.exception.FileClientException;
import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.repository.BackupAgentJobRepository;
import de.evoila.cf.backup.repository.FileDestinationRepository;
import de.evoila.cf.backup.service.exception.BackupRequestException;
import de.evoila.cf.model.*;
import de.evoila.cf.model.enums.BackupType;
import de.evoila.cf.model.enums.DestinationType;
import de.evoila.cf.model.enums.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Service
public class BackupServiceManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private List<BackupExecutorService> services = new ArrayList<>();

    private ThreadPoolTaskExecutor taskExecutor;

    private BackupAgentJobRepository backupAgentJobRepository;

    private FileDestinationRepository fileDestinationRepository;

    public BackupServiceManager(BackupAgentJobRepository jobRepository, FileDestinationRepository destRepoisitory) {
        this.backupAgentJobRepository = jobRepository;
        this.fileDestinationRepository = destRepoisitory;
    }

    public void addBackupServiceManager(BackupExecutorService service) {
        this.services.add(service);
    }

    @PostConstruct
    private void postConstruct() {
        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(2);
        taskExecutor.setMaxPoolSize(10);
        taskExecutor.initialize();
    }

    public List<BackupExecutorService> getServices() {
        return Collections.unmodifiableList(services);
    }

    public BackupJob backup(BackupRequest backupRequest) throws BackupRequestException, BackupException {
       FileDestination destination = fileDestinationRepository.findById(backupRequest.getPlan()
               .getDestinationId()).orElse(null);

        if (destination == null)
            throw new BackupException("Did not find destination with ID=" + backupRequest.getPlan().getDestinationId());

        return backup(backupRequest.getPlan(), destination);
    }


    public BackupJob backup(BackupPlan plan, FileDestination destination) throws BackupRequestException {
        BackupJob job = new BackupJob(BackupJob.BACKUP_JOB, destination.getServiceInstanceId(), JobStatus.STARTED);
        backupAgentJobRepository.save(job);

        Optional<BackupExecutorService> backupExecutorService = this
                .getBackupService(plan.getSource().getType(), destination.getType());

        if (!backupExecutorService.isPresent()) {
            String msg = String.format("No Backup Service found (JOB=%s) for Database %s",
                    job.getId(),
                    plan.getSource().getType())
                    + getServices()
                    .stream()
                    .map(s -> s.getSourceType().toString())
                    .collect(Collectors.toList());
            log.warn(msg);
            job.appendLog(msg);
            job.setStatus(JobStatus.FAILED);
            backupAgentJobRepository.save(job);
            throw new BackupRequestException("No Backup Service found");
        }

        taskExecutor.execute(() -> executeBackup(backupExecutorService.get(), job, plan, destination));
        return job;
    }


    private void executeBackup(BackupExecutorService backupExecutorService, BackupJob job, BackupPlan plan, FileDestination destination) {
        try {
            job.setStatus(JobStatus.IN_PROGRESS);
            backupAgentJobRepository.save(job);
            backupExecutorService.backup(plan, destination, job);
            job.setFileDestination(destination);
            job.setStatus(JobStatus.FINISHED);
            backupAgentJobRepository.save(job);
        } catch (Exception e) {
            String msg = String.format("An error occured (%s) : %s", job.getId(), e.getMessage());
            log.error(msg);
            job.appendLog(msg);
            e.printStackTrace();
            job.setStatus(JobStatus.FAILED);
            backupAgentJobRepository.save(job);
        }
    }

    public BackupJob restore(RestoreRequest restoreRequest) throws BackupRequestException {
        return restore(restoreRequest.getPlan().getSource(), restoreRequest.getSource());
    }

    public BackupJob restore(EndpointCredential destination, FileDestination source) throws BackupRequestException {
        BackupJob job = new BackupJob(BackupJob.RESTORE_JOB, destination.getServiceInstanceId(), JobStatus.STARTED);
        backupAgentJobRepository.save(job);
        Optional<BackupExecutorService> backupExecutorService = this.getServices().stream()
                .filter(s -> s.getSourceType().equals(destination.getType()))
                .filter(s -> s.getDestinationTypes().contains(DestinationType.SWIFT))
                .findAny();
        if (!backupExecutorService.isPresent()) {
            String msg = String.format("No Backup Service found (JOB=%s) for Database %s", job.getId(), destination.getType())
                    + getServices().stream().map(s -> s.getSourceType().toString()).collect(Collectors.toList());
            log.warn(msg);
            job.appendLog(msg);
            job.setStatus(JobStatus.FAILED);
            backupAgentJobRepository.save(job);
            throw new BackupRequestException("No Backupservice found");
        }
        taskExecutor.execute(() -> {
            executeRestore(backupExecutorService.get(), job, destination, source);
        });
        return job;
    }

    private void executeRestore(BackupExecutorService service, BackupJob job, EndpointCredential destination, FileDestination source) {
        try {
            job.setStatus(JobStatus.IN_PROGRESS);
            backupAgentJobRepository.save(job);
            service.restore(destination, source, job);
            job.setStatus(JobStatus.FINISHED);
            backupAgentJobRepository.save(job);
        } catch (BackupException | IOException | FileClientException | InterruptedException e) {
            e.printStackTrace();
            log.error(String.format("An error occured (%s) : [%s]   %s", job.getId(), e.getClass(), e.getMessage()));
            job.setStatus(JobStatus.FAILED);
            backupAgentJobRepository.save(job);
        }
    }

    private Optional<BackupExecutorService> getBackupService(BackupType sourceType, DestinationType destType) {
        Optional<BackupExecutorService> service = this.getServices()
                .stream()
                .filter(s -> s.getSourceType().equals(sourceType))
                .filter(s -> s.getDestinationTypes().contains(destType))
                .findFirst();

        return service;
    }

}
