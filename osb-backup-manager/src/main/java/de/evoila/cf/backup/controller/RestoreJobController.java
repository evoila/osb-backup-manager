package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.repository.AbstractJobRepository;
import de.evoila.cf.model.api.AbstractJob;
import de.evoila.cf.model.enums.JobType;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Controller
public class RestoreJobController {

    AbstractJobRepository abstractJobRepository;

    public RestoreJobController(AbstractJobRepository abstractJobRepository) {
        this.abstractJobRepository = abstractJobRepository;
    }

    @RequestMapping(value = "/restoreJobs/{jobId}", method = RequestMethod.GET)
    public ResponseEntity<AbstractJob> get(@PathVariable ObjectId jobId) {
        AbstractJob job = abstractJobRepository.findById(jobId).orElse(null);
        return new ResponseEntity<>(job, HttpStatus.OK);
    }

    @RequestMapping("/restoreJobs/byInstance/{instanceId}")
    public ResponseEntity<Page<AbstractJob>> all(@PathVariable String instanceId,
                                               @PageableDefault(size = 50, page = 0) Pageable pageable) {
        Page<AbstractJob> jobs = abstractJobRepository.findByServiceInstanceIdAndJobType(instanceId,
                JobType.RESTORE, pageable);
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

}
