package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.repository.BackupAgentJobRepository;
import de.evoila.cf.backup.service.BackupCleanupManager;
import de.evoila.cf.model.BackupJob;
import de.evoila.cf.model.FileDestination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Controller
public class JobController {

    BackupAgentJobRepository jobRepository;

    BackupCleanupManager backupCleanupManager;

    public JobController(BackupAgentJobRepository backupAgentJobRepository, BackupCleanupManager backupCleanupManager) {
        this.jobRepository = backupAgentJobRepository;
        this.backupCleanupManager = backupCleanupManager;
    }

    @RequestMapping(value = "/jobs/{jobId}", method = RequestMethod.GET)
    public ResponseEntity<BackupJob> get(@PathVariable String jobId) {
        BackupJob job = jobRepository.findById(jobId).orElse(null);
        return new ResponseEntity<>(job, HttpStatus.OK);
    }

    @RequestMapping("/jobs/byInstance/{instanceId}")
    public ResponseEntity<Page<BackupJob>> all(@PathVariable String instanceId,
                                               @PageableDefault(size = 50, page = 0) Pageable pageable) {
        Page<BackupJob> jobs = jobRepository.findByServiceInstanceId(instanceId, pageable);
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    @RequestMapping(value = "/jobs/{jobId}", method = RequestMethod.DELETE)
    public ResponseEntity delete(@PathVariable String jobId) {
        BackupJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        jobRepository.delete(job);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/jobs/{jobId}/file", method = RequestMethod.DELETE)
    public ResponseEntity deleteFile(@PathVariable String jobId, @RequestBody FileDestination destination) {
        BackupJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        backupCleanupManager.delete(job, destination);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

}
