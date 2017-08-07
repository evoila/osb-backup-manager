package de.evoila.cf.controller;

import de.evoila.cf.model.BackupJob;
import de.evoila.cf.model.FileDestination;
import de.evoila.cf.openstack.OSException;
import de.evoila.cf.repository.BackupAgentJobRepository;
import de.evoila.cf.service.BackupServiceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.util.List;

/**
 * Created by yremmet on 06.07.17.
 */

@Controller

public class JobController extends BaseController {

    @Autowired
    BackupAgentJobRepository jobRepository;
    @Autowired
    BackupServiceManager backupServiceManager;

    @RequestMapping(value = "/jobs/{jobid}", method = RequestMethod.GET)
    public ResponseEntity<BackupJob> getJobUpdate (@PathVariable String jobid) {
        BackupJob job = jobRepository.findOne(jobid);
        return new ResponseEntity<BackupJob>(job, HttpStatus.OK);
    }

    @RequestMapping("/jobs/byInstance/{instance}")
    public ResponseEntity<Page<BackupJob>> getByInstance (@PathVariable String instance,
                                                          @PageableDefault(size = 50, page = 0) Pageable pageable) {

        List<BackupJob> jobs = jobRepository.findByInstanceId(instance, pageable);
        Page<BackupJob> pageI = new PageImpl<BackupJob>(jobs);
        return new ResponseEntity<Page<BackupJob>>(pageI, HttpStatus.OK);
    }

    @RequestMapping(value = "/jobs/{jobid}", method = RequestMethod.DELETE)
    public ResponseEntity deleteJob (@PathVariable String jobid)
          throws IOException, OSException {
        BackupJob job = jobRepository.findOne(jobid);
        if (job == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        jobRepository.delete(job);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }
    @RequestMapping(value = "/jobs/{jobid}/file", method = RequestMethod.DELETE)
    public ResponseEntity getJobUpdate (@PathVariable String jobid, @RequestBody FileDestination destination)
          throws IOException, OSException {

        BackupJob job = jobRepository.findOne(jobid);
        if (job == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        backupServiceManager.delete(job, destination);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

}
