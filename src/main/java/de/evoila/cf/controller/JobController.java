package de.evoila.cf.controller;

import de.evoila.cf.model.BackupJob;
import de.evoila.cf.model.FileDestination;
import de.evoila.cf.openstack.OSException;
import de.evoila.cf.repository.BackupAgentJobRepository;
import de.evoila.cf.service.BackupServiceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * Created by yremmet on 06.07.17.
 */

@Controller

public class JobController {

    @Autowired
    BackupAgentJobRepository jobRepository;
    @Autowired
    BackupServiceManager backupServiceManager;

    @RequestMapping(value = "/job/{jobid}", method = RequestMethod.GET)
    public ResponseEntity<BackupJob> getJobUpdate (@PathVariable String jobid) {
        BackupJob job = jobRepository.findOne(jobid);
        return new ResponseEntity<BackupJob>(job, HttpStatus.OK);
    }

    @RequestMapping("/jobs/byInstance/{instance}")
    public ResponseEntity<List<BackupJob>> getByInstance (@PathVariable String instance,
                                                          @RequestParam(value = "page_size", defaultValue = "25") int pageSize,
                                                          @RequestParam(value = "page", defaultValue = "0") int page) {
        Pageable pageable = new PageRequest(page, pageSize, new Sort(Sort.Direction.ASC, "startDate"));
        List<BackupJob> jobs = jobRepository.findByInstanceId(instance, pageable);
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

    @RequestMapping(value = "/job/{jobid}", method = RequestMethod.DELETE)
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
