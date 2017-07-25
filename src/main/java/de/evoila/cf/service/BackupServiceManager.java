package de.evoila.cf.service;

import de.evoila.cf.model.*;
import de.evoila.cf.openstack.OSException;
import de.evoila.cf.repository.BackupAgentJobRepository;
import de.evoila.cf.service.exception.BackupRequestException;
import de.evoila.cf.service.exception.ProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

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

  public BackupJob backup (BackupRequest backupRequest) throws BackupRequestException {
    return backup(backupRequest.getSource(), backupRequest.getDestination());
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
    } catch (IOException | OSException | ProcessException | InterruptedException e) {
      log.error(String.format("An error occured (%s) : %s", job.getDestination(), e.getMessage()));
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
                                            .filter(s -> s.getDestinationTypes().contains(source.getType()))
                                            .findAny();
    if (! service.isPresent()) {
      log.warn(String.format("No Backup Service found (JOB=%s) for Database %s", job.getId(),destination.getType()));
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
    } catch (IOException | OSException | ProcessException | InterruptedException e) {
      e.printStackTrace();
      log.error(String.format("An error occured (%s) : %s", job.getDestination(), e.getMessage()));
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
    Optional<BackupService> service = this.getServices()
                                            .stream()
                                            .filter(s -> s.getSourceType().equals(source.getType()))
                                            .filter(s -> s.getDestinationTypes().contains(destination.getType()))
                                            .findAny();
    if (! service.isPresent()) {
      log.warn(String.format("No Backup Service found (JOB=%s) for Database %s", job.getId(),source.getType()));
      job.setStatus(JobStatus.FAILED);
      jobRepository.save(job);
      throw new BackupRequestException("No Backupservice found");
    }
    taskExecutor.execute(() -> executeBackup(source,destination, service.get(), job));
    return job;
  }

  public void removeOldBackupFiles (BackupPlan plan) {

  }
}
