package de.evoila.cf.backup.service;

import de.evoila.cf.backup.clients.S3Client;
import de.evoila.cf.backup.clients.SwiftClient;
import de.evoila.cf.backup.repository.BackupAgentJobRepository;
import de.evoila.cf.backup.repository.FileDestinationRepository;
import de.evoila.cf.model.*;
import de.evoila.cf.model.enums.DestinationType;
import de.evoila.cf.model.enums.JobStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Service
public class BackupCleanupManager {

    private BackupAgentJobRepository backupAgentJobRepository;

    private FileDestinationRepository fileDestinationRepository;

    public BackupCleanupManager(BackupAgentJobRepository jobRepository, FileDestinationRepository destRepoisitory) {
        this.backupAgentJobRepository = jobRepository;
        this.fileDestinationRepository = destRepoisitory;
    }

    public void removeOldBackupFiles(BackupPlan plan) {
        switch (plan.getRetentionStyle()) {
            case FILES:
                removeOldBackupByFiles(plan);
                break;
            case DAYS:
                removeOldBackupByTime(plan, ChronoUnit.DAYS);
                break;
            case HOURS:
                removeOldBackupByTime(plan, ChronoUnit.HOURS);
                break;
            case ALL:
            default:
        }
    }

    private void removeOldBackupByFiles(BackupPlan plan) {
        List<BackupJob> jobs = getJobs(plan);
        FileDestination destination = fileDestinationRepository.findById(plan.getDestinationId()).orElse(null);
        while (jobs.size() > plan.getRetentionPeriod()) {
            BackupJob job = jobs.get(0);
            jobs.remove(job);
            plan.getJobIds().remove(job.getId());

            delete(job, destination);
        }
    }

    private List<BackupJob> getJobs(BackupPlan plan) {
        List<String> jobIds = plan.getJobIds();
        Iterable<BackupJob> jobsIterator = backupAgentJobRepository.findAllById(jobIds);
        List<BackupJob> jobs = StreamSupport.stream(jobsIterator.spliterator(), false)
                .filter(job -> job.getStatus().equals(JobStatus.FINISHED))
                .filter(job -> job.getJobType().equals(BackupJob.BACKUP_JOB))
                .sorted((o1, o2) -> o1.getStartDate().compareTo(o2.getStartDate()))
                .collect(Collectors.toList());
        return jobs;
    }


    private void removeOldBackupByTime(BackupPlan plan, TemporalUnit unit) {
        FileDestination destination = fileDestinationRepository.findById(plan.getDestinationId()).orElse(null);

        List<BackupJob> jobs = getJobs(plan).stream()
                .filter(j -> Instant.now().isAfter(j.getStartDate().toInstant().plus(plan.getRetentionPeriod(),
                        unit)))
                .collect(Collectors.toList());

        for (BackupJob job : jobs) {
            delete(job, destination);
        }
    }

    public void delete(BackupJob job, FileDestination destination) {
        if (destination.getType().equals(DestinationType.SWIFT))
            deleteSwift((SwiftFileDestination) destination);

        if (destination.getType().equals(DestinationType.S3))
            deleteS3((S3FileDestination) destination);
        backupAgentJobRepository.deleteById(job.getId());

    }

    private void deleteS3(S3FileDestination destination) {
        S3Client s3Client = new S3Client(destination.getRegion(),
                destination.getUsername(),
                destination.getPassword());
        s3Client.delete(destination.getBucket(), destination.getFilename());
    }

    private void deleteSwift(SwiftFileDestination destination) {
        SwiftClient swiftClient = new SwiftClient(destination.getAuthUrl(),
                destination.getUsername(),
                destination.getPassword(),
                destination.getDomain(),
                destination.getProjectName()
        );
        swiftClient.delete(destination.getContainerName(), destination.getFilename());
    }
}
