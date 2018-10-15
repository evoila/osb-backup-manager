package de.evoila.cf.service;

import de.evoila.cf.controller.exception.BackupException;
import de.evoila.cf.model.*;
import de.evoila.cf.model.enums.BackupType;
import de.evoila.cf.model.enums.DestinationType;
import de.evoila.cf.model.enums.JobStatus;
import de.evoila.cf.backup.clients.exception.SwiftClientException;
import de.evoila.cf.backup.clients.SwiftClient;
import de.evoila.cf.repository.BackupAgentJobRepository;
import de.evoila.cf.repository.FileDestinationRepository;
import de.evoila.cf.service.backup.BackupService;
import de.evoila.cf.service.exception.BackupRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by yremmet on 27.06.17.
 */
@Service
public class BackupServiceManager {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private List<BackupService> services = new ArrayList<>();

    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private BackupAgentJobRepository jobRepository;

    @Autowired
    private FileDestinationRepository destRepoisitory;

    public void addBackupServiceManager(BackupService service) {
        this.services.add(service);
    }

    @PostConstruct
    private void postConstruct() {
        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(2);
        taskExecutor.setMaxPoolSize(10);
        taskExecutor.initialize();
    }

    public List<BackupService> getServices() {
        return Collections.unmodifiableList(services);
    }

    public BackupJob backup(BackupRequest backupRequest) throws BackupRequestException, BackupException {
        if (backupRequest == null) {
            throw new BackupException("Backup Request is null");
        }
        FileDestination destination = destRepoisitory.findById(backupRequest.getDestinationId()).orElse(null);
        if (destination == null)
            throw new BackupException("Did not find destination with ID=" + backupRequest.getDestinationId());
        return backup(backupRequest.getPlan(), destination);
    }

    private void executeBackup(BackupPlan plan, FileDestination destination, BackupService service, BackupJob job) {
        try {
            job.setStatus(JobStatus.IN_PROGRESS);
            jobRepository.save(job);
            Map<String, String> fileNames = service.backup(plan, destination, job);
            destination.setFilenames(fileNames);
            job.setBackupFile(destination);
            job.setStatus(JobStatus.FINISHED);
            jobRepository.save(job);
        } catch (Exception e) {
            String msg = String.format("An error occured (%s) : %s", job.getId(), e.getMessage());
            log.error(msg);
            job.appendLog(msg);
            e.printStackTrace();
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);
        }
    }

    public BackupJob restore(RestoreRequest restoreRequest) throws BackupRequestException {
        return restore(restoreRequest.getPlan().getSource(), restoreRequest.getSource());
    }

    public BackupJob restore(EndpointCredential destination, FileDestination source) throws BackupRequestException {
        BackupJob job = new BackupJob();
        job.setJobType(BackupJob.RESTORE_JOB);
        job.setInstanceId(destination.getServiceInstanceId());
        job.setStatus(JobStatus.STARTED);
        job.setStartDate(new Date());
        jobRepository.save(job);
        Optional<BackupService> service = this.getServices().stream()
                .filter(s -> s.getSourceType().equals(destination.getType()))
                .filter(s -> s.getDestinationTypes().contains(DestinationType.SWIFT))
                .findAny();
        if (!service.isPresent()) {
            String msg = String.format("No Backup Service found (JOB=%s) for Database %s", job.getId(), destination.getType())
                    + getServices().stream().map(s -> s.getSourceType().toString()).collect(Collectors.toList());
            log.warn(msg);
            job.appendLog(msg);
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);
            throw new BackupRequestException("No Backupservice found");
        }
        taskExecutor.execute(() -> {
            executeRestore(destination, source, service.get(), job);
        });
        return job;
    }

    private void executeRestore(EndpointCredential source, FileDestination destination, BackupService service, BackupJob job) {
        try {
            job.setStatus(JobStatus.IN_PROGRESS);
            jobRepository.save(job);
            service.restore(source, destination, job);
            job.setStatus(JobStatus.FINISHED);
            jobRepository.save(job);
        } catch (BackupException | IOException | SwiftClientException | InterruptedException e) {
            e.printStackTrace();
            log.error(String.format("An error occured (%s) : [%s]   %s", job.getId(), e.getClass(), e.getMessage()));
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);
        }
    }

    public BackupJob backup(BackupPlan plan, FileDestination destination) throws BackupRequestException {

        EndpointCredential endpointCredential = plan.getSource();
        BackupJob job = new BackupJob();
        job.setJobType(BackupJob.BACKUP_JOB);
        job.setInstanceId(endpointCredential.getServiceInstanceId());
        job.setStatus(JobStatus.STARTED);
        job.setStartDate(new Date());
        jobRepository.save(job);
        Optional<BackupService> service = this.getBackupService(endpointCredential.getType(), destination.getType());

        if (!service.isPresent()) {
            String msg = String.format("No Backup Service found (JOB=%s) for Database %s",
                    job.getId(),
                    endpointCredential.getType())
                    + getServices()
                    .stream()
                    .map(s -> s.getSourceType().toString())
                    .collect(Collectors.toList());
            log.warn(msg);
            job.appendLog(msg);
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);
            throw new BackupRequestException("No Backup Service found");
        }

        taskExecutor.execute(() -> executeBackup(plan, destination, service.get(), job));
        return job;
    }

    private Optional<BackupService> getBackupService(BackupType sourceType, DestinationType destType) {
        Optional<BackupService> service = this.getServices()
                .stream()
                .filter(s -> s.getSourceType().equals(sourceType))
                .filter(s -> s.getDestinationTypes().contains(destType))
                .findFirst();

        return service;
    }

    public void removeOldBackupFiles(BackupPlan plan) throws IOException, SwiftClientException {
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

    private void removeOldBackupByFiles(BackupPlan plan) throws IOException, SwiftClientException {
        List<BackupJob> jobs = getJobs(plan);
        FileDestination destination = destRepoisitory.findById(plan.getDestinationId()).orElse(null);
        while (jobs.size() > plan.getRetentionPeriod()) {
            BackupJob job = jobs.get(0);
            jobs.remove(job);
            plan.getJobIds().remove(job.getId());

            delete(job, destination);
        }
    }

    private List<BackupJob> getJobs(BackupPlan plan) {
        List<String> jobIds = plan.getJobIds();
        Iterable<BackupJob> jobsIterator = jobRepository.findAllById(jobIds);
        List<BackupJob> jobs = StreamSupport.stream(jobsIterator.spliterator(), false)
                .filter(job -> job.getStatus().equals(JobStatus.FINISHED))
                .filter(job -> job.getJobType().equals(BackupJob.BACKUP_JOB))
                .sorted((o1, o2) -> o1.getStartDate().compareTo(o2.getStartDate()))
                .collect(Collectors.toList());
        return jobs;
    }


    private void removeOldBackupByTime(BackupPlan plan, TemporalUnit unit) throws IOException, SwiftClientException {
        FileDestination destination = destRepoisitory.findById(plan.getDestinationId()).orElse(null);

        List<BackupJob> jobs = getJobs(plan).stream()
                .filter(j -> Instant.now().isAfter(j.getStartDate().toInstant().plus(plan.getRetentionPeriod(),
                        unit)))
                .collect(Collectors.toList());

        for (BackupJob job : jobs) {
            delete(job, destination);
        }
    }

    public void delete(BackupJob job, FileDestination destination) throws IOException, SwiftClientException {
        SwiftClient swiftClient = new SwiftClient(destination.getAuthUrl(),
                destination.getUsername(),
                destination.getPassword(),
                destination.getDomain(),
                destination.getProjectName()
        );

        for (Map.Entry<String, String> filename : destination.getFilenames().entrySet()) {
            try {
                //swiftClient.delete(job.getDestination().getContainer(), filename.getValue());
                jobRepository.deleteById(job.getId());
            } catch (Exception e) {
                log.error(String.format("Could not remove old Backups [File %s] : %s", filename.getValue(), e.getMessage()));
            }
        }
    }

}
