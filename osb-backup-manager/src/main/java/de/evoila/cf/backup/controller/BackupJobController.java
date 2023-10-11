package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.repository.AbstractJobRepository;
import de.evoila.cf.backup.service.BackupCleanupManager;
import de.evoila.cf.model.api.AbstractJob;
import de.evoila.cf.model.api.file.FileDestination;
import de.evoila.cf.model.enums.JobStatus;
import de.evoila.cf.model.enums.JobType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "/backupJobs",
        description = """
                Get or delete backup jobs from the job repository (MongoDB), which are or have been \
                processed by the Backup Agent.\
                """)
@Controller
public class BackupJobController {

    AbstractJobRepository abstractJobRepository;

    BackupCleanupManager backupCleanupManager;

    public BackupJobController(AbstractJobRepository abstractJobRepository, BackupCleanupManager backupCleanupManager) {
        this.abstractJobRepository = abstractJobRepository;
        this.backupCleanupManager = backupCleanupManager;
    }

    @Operation(summary = "Gets a job from the repository.")
    @GetMapping("/backupJobs/{jobId}")
    public ResponseEntity<AbstractJob> get(@PathVariable ObjectId jobId) {
        AbstractJob job = abstractJobRepository.findById(jobId).orElse(null);
        return new ResponseEntity(job, HttpStatus.OK);
    }

    @Operation(summary = "Gets a page of jobs for the specified service instance.")
    @GetMapping("/backupJobs/byInstance/{serviceInstanceId}")
    public ResponseEntity<Page<AbstractJob>> all(@PathVariable String serviceInstanceId,
                                                 @PageableDefault(size = 10, sort = {"startDate"},
                                                         direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AbstractJob> jobs = abstractJobRepository.findByServiceInstanceIdAndJobType(serviceInstanceId,
                JobType.BACKUP, pageable);
        return new ResponseEntity(jobs, HttpStatus.OK);
    }

    @Operation(summary = "Gets a page of jobs for the specified service instance, filtered by their JobStatus.")
    @GetMapping("/backupJobs/byInstance/{serviceInstanceId}/filtered")
    public ResponseEntity<Page<AbstractJob>> allFiltered(@PathVariable String serviceInstanceId,
                                                         @RequestParam JobStatus jobStatus,
                                                         @PageableDefault(size = 10, sort = {"startDate"},
                                                                 direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AbstractJob> jobs = abstractJobRepository.findByServiceInstanceIdAndJobTypeAndStatus(serviceInstanceId,
                JobType.BACKUP, jobStatus, pageable);
        return new ResponseEntity(jobs, HttpStatus.OK);
    }

    @Operation(summary = "Deletes a job from the repository.")
    @DeleteMapping("/backupJobs/{jobId}")
    public ResponseEntity delete(@PathVariable ObjectId jobId) {
        AbstractJob job = abstractJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        abstractJobRepository.delete(job);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Deletes all jobs from the specified service instance.")
    @DeleteMapping("/backupJobs/byInstance/{serviceInstanceId}")
    public ResponseEntity deleteByInstance(@PathVariable String serviceInstanceId) {
        abstractJobRepository.deleteByServiceInstanceId(serviceInstanceId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    @Operation(summary = "Delete the file backed up by this job.")
    @DeleteMapping("/backupJobs/{jobId}/file")
    public ResponseEntity deleteFile(@PathVariable ObjectId jobId, @RequestBody FileDestination destination) {
        AbstractJob job = abstractJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        backupCleanupManager.delete(job, destination);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

}
