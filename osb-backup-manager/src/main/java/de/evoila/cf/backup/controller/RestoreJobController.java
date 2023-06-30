package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.repository.AbstractJobRepository;
import de.evoila.cf.model.api.AbstractJob;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Api(value = "/restoreJobs",
        description = "Restores backup jobs from the JobRepository (e.g. MongoDB).")
@Controller
public class RestoreJobController {

    AbstractJobRepository abstractJobRepository;

    public RestoreJobController(AbstractJobRepository abstractJobRepository) {
        this.abstractJobRepository = abstractJobRepository;
    }

    @ApiOperation(value = "Gets a job with the given id from the repository.")
    @RequestMapping(value = "/restoreJobs/{jobId}", method = RequestMethod.GET)
    public ResponseEntity<AbstractJob> get(@PathVariable ObjectId jobId) {
        AbstractJob job = abstractJobRepository.findById(jobId).orElse(null);
        return new ResponseEntity<>(job, HttpStatus.OK);
    }

    @ApiOperation(value = "Gets a page of configured jobs for the specified service instance.")
    @RequestMapping(value = "/restoreJobs/byInstance/{serviceInstanceId}", method = RequestMethod.GET)
    public ResponseEntity<Page<AbstractJob>> all(@PathVariable String serviceInstanceId,
                                                 @PageableDefault(size = 10,sort = {"startDate"},
                                                     direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AbstractJob> jobs = abstractJobRepository.findByServiceInstanceIdAndJobType(serviceInstanceId,
                JobType.RESTORE, pageable);
        return new ResponseEntity<>(jobs, HttpStatus.OK);
    }

}
