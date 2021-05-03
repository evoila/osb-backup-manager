package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.repository.AbstractJobRepository;
import de.evoila.cf.model.api.AbstractJob;
import de.evoila.cf.model.enums.JobType;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Controller
public class RestoreJobController {

    AbstractJobRepository abstractJobRepository;

    public RestoreJobController(AbstractJobRepository abstractJobRepository) {
        this.abstractJobRepository = abstractJobRepository;
    }

    @GetMapping(value = "/restoreJobs/{jobId}")
    public ResponseEntity<AbstractJob> get(@PathVariable ObjectId jobId) {
        AbstractJob job = abstractJobRepository.findById(jobId).orElse(null);
        return new ResponseEntity<>(job, HttpStatus.OK);
    }

    @GetMapping("/restoreJobs/byInstance/{instanceId}")
    public ResponseEntity<Page<AbstractJob>> all(@PathVariable String instanceId,
                                                 @PageableDefault(size = 10,sort = {"startDate"},
                                                     direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AbstractJob> jobs = abstractJobRepository.findByServiceInstanceIdAndJobType(instanceId,
                JobType.RESTORE, pageable);
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

}
