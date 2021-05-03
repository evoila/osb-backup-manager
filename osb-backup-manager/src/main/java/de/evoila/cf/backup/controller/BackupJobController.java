package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.repository.AbstractJobRepository;
import de.evoila.cf.backup.service.BackupCleanupManager;
import de.evoila.cf.model.api.AbstractJob;
import de.evoila.cf.model.api.file.FileDestination;
import de.evoila.cf.model.enums.JobStatus;
import de.evoila.cf.model.enums.JobType;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Controller
public class BackupJobController {

    AbstractJobRepository abstractJobRepository;

    BackupCleanupManager backupCleanupManager;

    public BackupJobController(AbstractJobRepository abstractJobRepository, BackupCleanupManager backupCleanupManager) {
        this.abstractJobRepository = abstractJobRepository;
        this.backupCleanupManager = backupCleanupManager;
    }

    @GetMapping(value = "/backupJobs/{jobId}")
    public ResponseEntity<AbstractJob> get(@PathVariable ObjectId jobId) {
        AbstractJob job = abstractJobRepository.findById(jobId).orElse(null);
        return new ResponseEntity(job, HttpStatus.OK);
    }

    @GetMapping("/backupJobs/byInstance/{instanceId}")
    public ResponseEntity<Page<AbstractJob>> all(@PathVariable String instanceId,
                                               @PageableDefault(size = 10,sort = {"startDate"},
                                                       direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AbstractJob> jobs = abstractJobRepository.findByServiceInstanceIdAndJobType(instanceId,
                JobType.BACKUP, pageable);
        return new ResponseEntity(jobs, HttpStatus.OK);
    }

    @GetMapping("/backupJobs/byInstance/{instanceId}/filtered")
    public ResponseEntity<Page<AbstractJob>> allFiltered(@PathVariable String instanceId,
                                                 @RequestParam JobStatus jobStatus,
                                                 @PageableDefault(size = 10,sort = {"startDate"},
                                                     direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AbstractJob> jobs = abstractJobRepository.findByServiceInstanceIdAndJobTypeAndStatus(instanceId,
                JobType.BACKUP, jobStatus, pageable);
        return new ResponseEntity(jobs, HttpStatus.OK);
    }

    @DeleteMapping(value = "/backupJobs/{jobId}")
    public ResponseEntity delete(@PathVariable ObjectId jobId) {
        AbstractJob job = abstractJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        abstractJobRepository.delete(job);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(value = "/backupJobs/byInstance/{serviceInstanceId}")
    public ResponseEntity deleteByInstance(@PathVariable String serviceInstanceId) {
        abstractJobRepository.deleteByServiceInstanceId(serviceInstanceId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(value = "/backupJobs/{jobId}/file")
    public ResponseEntity deleteFile(@PathVariable ObjectId jobId, @RequestBody FileDestination destination) {
        AbstractJob job = abstractJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        backupCleanupManager.delete(job, destination);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

}
