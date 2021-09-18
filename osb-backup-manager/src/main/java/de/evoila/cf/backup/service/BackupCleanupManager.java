package de.evoila.cf.backup.service;

import de.evoila.cf.backup.clients.S3Client;
import de.evoila.cf.backup.clients.SwiftClient;
import de.evoila.cf.backup.repository.AbstractJobRepository;
import de.evoila.cf.backup.repository.FileDestinationRepository;
import de.evoila.cf.model.agent.response.AgentBackupResponse;
import de.evoila.cf.model.api.AbstractJob;
import de.evoila.cf.model.api.BackupPlan;
import de.evoila.cf.model.api.file.FileDestination;
import de.evoila.cf.model.api.file.S3FileDestination;
import de.evoila.cf.model.api.file.SwiftFileDestination;
import de.evoila.cf.model.enums.DestinationType;
import de.evoila.cf.model.enums.JobStatus;
import de.evoila.cf.model.enums.JobType;
import io.minio.errors.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 *
 * The BackupCleanupManager removes old backup files from Amazon S3 or OpenStack Swift cloud storages, created by
 * BackupJobs which RetentionPeriod has been exceeded.
 */
@Service
public class BackupCleanupManager {

    private AbstractJobRepository abstractJobRepository;

    private FileDestinationRepository fileDestinationRepository;

    public BackupCleanupManager(AbstractJobRepository abstractJobRepository, FileDestinationRepository destRepoisitory) {
        this.abstractJobRepository = abstractJobRepository;
        this.fileDestinationRepository = destRepoisitory;
    }

    /**
     * Remove old backups belonging to a BackupPlan, which have exceeded the retention period.
     *
     * @param plan the BackupPlan to be cleaned up
     */
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

    /**
     * Delete old jobs and files associated with the given BackupPlan, which exceed the retention period.
     *
     * @param backupPlan A BackupPlan
     */
    private void removeOldBackupByFiles(BackupPlan backupPlan) {
        List<AbstractJob> jobs = this.getJobs(backupPlan);
        FileDestination destination = backupPlan.getFileDestination();

        while (jobs.size() > backupPlan.getRetentionPeriod()) {
            AbstractJob job = jobs.get(0);;
            delete(job, destination);
        }
    }

    /**
     * Get a list of jobs created by the BackupPlan.
     *
     * @param backupPlan
     * @return list of jobs associated with the BackupPlan
     */
    private List<AbstractJob> getJobs(BackupPlan backupPlan) {
        List<AbstractJob> jobsIterator = abstractJobRepository.findByBackupPlan(backupPlan);
        List<AbstractJob> jobs = StreamSupport.stream(jobsIterator.spliterator(), false)
                .filter(job -> job.getStatus().equals(JobStatus.SUCCEEDED))
                .filter(job -> job.getJobType().equals(JobType.BACKUP))
                .sorted((o1, o2) -> o1.getStartDate().compareTo(o2.getStartDate()))
                .collect(Collectors.toList());
        return jobs;
    }

    /**
     * Delete old jobs and files associated with the given BackupPlan, which are older than the timeunit given,
     * multiplied with the retention period configured in the BackupPlan.
     *
     * @param backupPlan A Backuplan
     * @param unit a time unit to be multiplied with the retention period
     */
    private void removeOldBackupByTime(BackupPlan backupPlan, TemporalUnit unit) {
        FileDestination destination = backupPlan.getFileDestination();

        List<AbstractJob> jobs = getJobs(backupPlan).stream()
                .filter(j -> Instant.now()
                .isAfter(j.getStartDate().toInstant()
                .plus(backupPlan.getRetentionPeriod(), unit)))
                .collect(Collectors.toList());

        for (AbstractJob job : jobs) {
            delete(job, destination);
        }
    }

    /**
     * Delete backup files created by the given job from the destination.
     *
     * @param job a job
     * @param destination configurations for a cloud storage
     */
    public void delete(AbstractJob job, FileDestination destination) {
        if (destination.getType().equals(DestinationType.SWIFT))
            deleteSwift((SwiftFileDestination) destination);

        if (destination.getType().equals(DestinationType.S3)) {
            job.getAgentExecutionReponses().entrySet().forEach(entry -> {
                String filenamePrefix = ((AgentBackupResponse) entry.getValue()).getFilenamePrefix();
                if (filenamePrefix == ""){
                    destination.setFilenamePrefix(job.getIdAsString() + "/");
                    filenamePrefix = job.getIdAsString() + "/";
                }
                String filename = ((AgentBackupResponse) entry.getValue()).getFilename();
                deleteS3((S3FileDestination) destination, filenamePrefix + filename);
            });
        }

        abstractJobRepository.delete(job);
    }

    /**
     * Delete backup files from a Amazon S3 cloud storage.
     *
     * @param destination configuration for a cloud storage
     * @param filename name of the backup file
     */
    private void deleteS3(S3FileDestination destination, String filename) {
        S3Client s3Client = new S3Client(destination.getEndpoint(),
                destination.getRegion(),
                destination.getAuthKey(),
                destination.getAuthSecret(),
                fileDestinationRepository);
        try {
            s3Client.delete(destination.getBucket(), filename);
        } catch (IOException | InvalidKeyException | InvalidResponseException | InsufficientDataException |
                 NoSuchAlgorithmException | ServerException | InternalException | XmlParserException |
                 ErrorResponseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete backup files from a OpenStack Swift cloud storage.
     *
     * @param destination configuration for a cloud storage
     */
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
