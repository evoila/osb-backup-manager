package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.repository.AbstractJobRepository;
import de.evoila.cf.backup.service.BackupCleanupManager;
import de.evoila.cf.model.api.AbstractJob;
import de.evoila.cf.model.api.file.FileDestination;
import de.evoila.cf.model.enums.JobStatus;
import de.evoila.cf.model.enums.JobType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@Api(value = "/backupJobs",
        description = "Get or delete backup jobs from the job repository (MongoDB), which are or have been " +
                "processed by the Backup Agent.")
@Controller
public class BackupJobController {

    AbstractJobRepository abstractJobRepository;

    BackupCleanupManager backupCleanupManager;

    public BackupJobController(AbstractJobRepository abstractJobRepository, BackupCleanupManager backupCleanupManager) {
        this.abstractJobRepository = abstractJobRepository;
        this.backupCleanupManager = backupCleanupManager;
    }

    @ApiOperation(value = "Gets a job from the repository.")
    @RequestMapping(value = "/backupJobs/{jobId}", method = RequestMethod.GET)
    public ResponseEntity<AbstractJob> get(@PathVariable ObjectId jobId) {
        AbstractJob job = abstractJobRepository.findById(jobId).orElse(null);
        return new ResponseEntity(job, HttpStatus.OK);
    }

    @ApiOperation(value = "Gets a page of jobs for the specified service instance.")
    @RequestMapping(value = "/backupJobs/byInstance/{serviceInstanceId}", method = RequestMethod.GET)
    public ResponseEntity<Page<AbstractJob>> all(@PathVariable String serviceInstanceId,
                                                 @PageableDefault(size = 10, sort = {"startDate"},
                                                         direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AbstractJob> jobs = abstractJobRepository.findByServiceInstanceIdAndJobType(serviceInstanceId,
                JobType.BACKUP, pageable);
        return new ResponseEntity(jobs, HttpStatus.OK);
    }

    @ApiOperation(value = "Gets a page of jobs for the specified service instance, filtered by their JobStatus.")
    @RequestMapping(value = "/backupJobs/byInstance/{serviceInstanceId}/filtered", method = RequestMethod.GET)
    public ResponseEntity<Page<AbstractJob>> allFiltered(@PathVariable String serviceInstanceId,
                                                         @RequestParam JobStatus jobStatus,
                                                         @PageableDefault(size = 10, sort = {"startDate"},
                                                                 direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AbstractJob> jobs = abstractJobRepository.findByServiceInstanceIdAndJobTypeAndStatus(serviceInstanceId,
                JobType.BACKUP, jobStatus, pageable);
        return new ResponseEntity(jobs, HttpStatus.OK);
    }

    @ApiOperation(value = "Deletes a job from the repository.")
    @RequestMapping(value = "/backupJobs/{jobId}", method = RequestMethod.DELETE)
    public ResponseEntity delete(@PathVariable ObjectId jobId) {
        AbstractJob job = abstractJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        abstractJobRepository.delete(job);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "Deletes all jobs from the specified service instance.")
    @RequestMapping(value = "/backupJobs/byInstance/{serviceInstanceId}", method = RequestMethod.DELETE)
    public ResponseEntity deleteByInstance(@PathVariable String serviceInstanceId) {
        abstractJobRepository.deleteByServiceInstanceId(serviceInstanceId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    @ApiOperation(value = "Delete the file backed up by this job.")
    @RequestMapping(value = "/backupJobs/{jobId}/file", method = RequestMethod.DELETE)
    public ResponseEntity deleteFile(@PathVariable ObjectId jobId, @RequestBody FileDestination destination) {
        AbstractJob job = abstractJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        backupCleanupManager.delete(job, destination);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

}
