package de.evoila.cf.service;

import de.evoila.cf.controller.exception.BackupException;
import de.evoila.cf.model.*;
import de.evoila.cf.model.enums.DatabaseType;
import de.evoila.cf.model.enums.DestinationType;
import de.evoila.cf.openstack.OSException;
import de.evoila.cf.openstack.SwiftClient;
import de.evoila.cf.repository.BackupAgentJobRepository;
import de.evoila.cf.repository.FileDestinationRepository;
import de.evoila.cf.service.exception.BackupRequestException;
import de.evoila.cf.service.exception.ProcessException;
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

  public void addBackupServiceManager (BackupService service) {
    this.services.add(service);
  }

  @PostConstruct
  private void postConstruct () {
    taskExecutor = new ThreadPoolTaskExecutor();
    taskExecutor.setCorePoolSize(2);
    taskExecutor.setMaxPoolSize(5);
    taskExecutor.initialize();

  }

  public List<BackupService> getServices () {
    return Collections.unmodifiableList(services);
  }

  public BackupJob backup (BackupRequest backupRequest) throws BackupRequestException, BackupException {
    FileDestination destination = destRepoisitory.findOne(backupRequest.getDestinationId());
    if(destination == null){
      throw new BackupException("Did not find destination with ID=" + backupRequest.getDestinationId());
    }
    return backup(backupRequest.getSource(), destination);
  }

  private void executeBackup (DatabaseCredential source, FileDestination destination, BackupService service, BackupJob job) {
    try {
      job.setStatus(JobStatus.IN_PROGRESS);
      jobRepository.save(job);
      String fileName = service.backup(source,destination, job);
      destination.setFilename(fileName);
      job.setBackupFile(destination);
      job.setStatus(JobStatus.FINISHED);
      jobRepository.save(job);
    } catch (BackupException | IOException | OSException | ProcessException | InterruptedException e) {
      log.error(String.format("An error occured (%s) : %s", job.getId(), e.getMessage()));
      job.appendLog(String.format("An error occured (%s) : %s", job.getId(), e.getMessage()));
      e.printStackTrace();
      job.setStatus(JobStatus.FAILED);
      jobRepository.save(job);
    }
  }


  public BackupJob restore (RestoreRequest restoreRequest) throws BackupRequestException {
    return restore(restoreRequest.getDestination(), restoreRequest.getSource());
  }

  public BackupJob restore (DatabaseCredential destination, FileDestination source) throws BackupRequestException {
    BackupJob job = new BackupJob();
    job.setJobType(BackupJob.RESTORE_JOB);
    job.setInstanceId(destination.getContext());
    job.setStatus(JobStatus.STARTED);
    job.setStartDate(new Date());
    jobRepository.save(job);
    Optional<BackupService> service = this.getServices().stream()
                                            .filter(s -> s.getSourceType().equals(destination.getType()))
                                            .filter(s -> s.getDestinationTypes().contains(DestinationType.Swift))
                                            .findAny();
    if (! service.isPresent()) {
      String msg = String.format("No Backup Service found (JOB=%s) for Database %s", job.getId(),destination.getType())
            + getServices().stream().map(s -> s.getSourceType().toString()).collect(Collectors.toList());
      log.warn(msg);
      job.appendLog(msg);
      job.setStatus(JobStatus.FAILED);
      jobRepository.save(job);
      throw new BackupRequestException("No Backupservice found");
    }
    taskExecutor.execute(() -> {
      executeRestore( destination,  source, service.get(), job);
    });

    return job;
  }

  private void executeRestore (DatabaseCredential source, FileDestination destination, BackupService service, BackupJob job) {
    try {
      job.setStatus(JobStatus.IN_PROGRESS);
      jobRepository.save(job);
      service.restore(source,destination, job);
      job.setStatus(JobStatus.FINISHED);
      jobRepository.save(job);
    } catch (BackupException | IOException | OSException | ProcessException | InterruptedException e) {
      e.printStackTrace();
      log.error(String.format("An error occured (%s) : %s", job.getId(), e.getMessage()));
      job.setStatus(JobStatus.FAILED);
      jobRepository.save(job);
    }
  }

  public BackupJob backup (DatabaseCredential source, FileDestination destination) throws BackupRequestException {
    BackupJob job = new BackupJob();
    job.setJobType(BackupJob.BACKUP_JOB);
    job.setInstanceId(source.getContext());
    job.setStatus(JobStatus.STARTED);
    job.setStartDate(new Date());
    jobRepository.save(job);
    Optional<BackupService> service = this.getBackupService(source.getType(), DestinationType.Swift);

    if (!service.isPresent()) {
      String msg = String.format("No Backup Service found (JOB=%s) for Database %s", job.getId(),source.getType().toString())
                         + getServices().stream().map(s -> s.getSourceType().toString()).collect(Collectors.toList());
      log.warn(msg);
      job.appendLog(msg);
      job.setStatus(JobStatus.FAILED);
      jobRepository.save(job);
      throw new BackupRequestException("No Backup Service found");
    }

    taskExecutor.execute(() -> executeBackup(source,destination, service.get(), job));
    return job;
  }

  private Optional<BackupService> getBackupService (DatabaseType sourceType, DestinationType destType) {
    Optional<BackupService> service = this.getServices()
                                            .stream()
                                            .filter(s -> s.getSourceType().equals(sourceType))
                                            .filter(s -> s.getDestinationTypes().contains(destType))
                                            .findFirst();
    if(!service.isPresent()){
      for(BackupService service1 : this.getServices()){
        if(service1.getSourceType().equals(sourceType)){
          service = Optional.of(service1);
        }
      }
    }
    return service;
  }

  public void removeOldBackupFiles (BackupPlan plan) throws IOException, OSException {
    switch (plan.getRetentionStyle()){
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

  private void removeOldBackupByFiles (BackupPlan plan) throws IOException, OSException {
    List<BackupJob> jobs = getJobs(plan);
    FileDestination destination = destRepoisitory.findOne(plan.getDestinationId());
    while(jobs.size() > plan.getRetentionPeriod()){
      BackupJob job = jobs.get(0);
      jobs.remove(job);
      plan.getJobIds().remove(job.getId());

      delete(job, destination);
    }

  }

  private List<BackupJob> getJobs (BackupPlan plan) {
    List<String> jobIds =plan.getJobIds();
    Iterable<BackupJob> jobsIterator = jobRepository.findAll(jobIds);
    ArrayList<BackupJob> jobs = new ArrayList<>();

    StreamSupport.stream(jobsIterator.spliterator(), false)
          .filter(job-> job.getStatus().equals(JobStatus.FINISHED))
          .filter(job-> job.getJobType().equals(BackupJob.BACKUP_JOB))
          .forEach(job -> jobs.add(job));
    Collections.sort(jobs, (o1, o2) -> o1.getStartDate().compareTo(o2.getStartDate()));
    return jobs;
  }


  private void removeOldBackupByTime (BackupPlan plan, TemporalUnit unit) throws IOException, OSException {
    FileDestination destination = destRepoisitory.findOne(plan.getDestinationId());

    List<BackupJob> jobs = getJobs(plan).stream()
                                 .filter(j -> Instant.now().isAfter(j.getStartDate().toInstant().plus(plan.getRetentionPeriod(),
                                                                                                      unit)))
                                 .collect(Collectors.toList());
    for(BackupJob job : jobs){
      delete(job, destination);
    }
  }



  public void delete (BackupJob job, FileDestination destination) throws IOException, OSException {
    SwiftClient swiftClient = new SwiftClient(destination.getAuthUrl(),
                                              destination.getUsername(),
                                              destination.getPassword(),
                                              destination.getDomain(),
                                              destination.getProjectName()
    );
    swiftClient.delete(job.getDestination().getContainer(), job.getDestination().getFilename());

    jobRepository.delete(job.getId());
  }

}
