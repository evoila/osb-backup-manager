package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.repository.BackupPlanRepository;
import de.evoila.cf.backup.service.BackupPlanService;
import de.evoila.cf.backup.service.permissions.PermissionCheckService;
import de.evoila.cf.model.api.BackupPlan;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Api(value = "/backupPlans", description = "Manage individual BackupPlans or BackupPlans grouped by service instances.")
@Controller
public class BackupPlanController {

    BackupPlanService backupPlanService;

    BackupPlanRepository backupPlanRepository;

    PermissionCheckService permissionCheckService;

    public BackupPlanController(BackupPlanService backupPlanService, BackupPlanRepository backupPlanRepository, PermissionCheckService permissionCheckService) {
        this.backupPlanService = backupPlanService;
        this.backupPlanRepository = backupPlanRepository;
        this.permissionCheckService = permissionCheckService;
    }

    @ApiOperation(value = "Get all BackupPlan from the specified service instance.")
    @RequestMapping(value = "/backupPlans/byInstance/{serviceInstanceId}", method = RequestMethod.GET)
    public ResponseEntity<Page<BackupPlan>> all(@PathVariable() String serviceInstanceId,
                                                @PageableDefault(size = 50, page = 0) Pageable pageable) {
        Page<BackupPlan> response = backupPlanService.getPlans(serviceInstanceId, pageable);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ApiOperation(value = "Save the BackupPlan in the repository and add it as a task in the scheduling service if " +
            "not paused. The task will periodically add backup jobs to the queue, as is configured in the BackupPlan.")
    @RequestMapping(value = "/backupPlans", method = RequestMethod.POST)
    public ResponseEntity<BackupPlan> create(@Valid @RequestBody BackupPlan plan) throws BackupException {

        String instanceID = plan.getServiceInstance().getId();
        if (!permissionCheckService.hasReadAccess(instanceID)) {
            throw new AuthenticationServiceException("User is not authorised to access the requested resource. Please contact your System Administrator.");
        }

        BackupPlan response = backupPlanService.createPlan(plan);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @ApiOperation(value = "Get the BackupPlan from the repository specified by its ID.")
    @RequestMapping(value = "/backupPlans/{planId}", method = RequestMethod.GET)
    public ResponseEntity<BackupPlan> get(@PathVariable() ObjectId planId) {
        BackupPlan response = backupPlanService.getPlan(planId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ApiOperation(value = "Delete the BackupPlan and running task from the repository and scheduling service.")
    @RequestMapping(value = "/backupPlans/{planId}", method = RequestMethod.DELETE)
    public ResponseEntity<BackupPlan> delete(@PathVariable() ObjectId planId) {
        BackupPlan response = backupPlanService.deletePlan(planId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @ApiOperation(value = "Delete all BackupPlans, running tasks from the repository and scheduling service.")
    @RequestMapping(value = "/backupPlans/byInstance/{serviceInstanceId}", method = RequestMethod.DELETE)
    public ResponseEntity deleteByInstance(@PathVariable String serviceInstanceId) {
        List<BackupPlan> plansToDelete = backupPlanRepository.findByServiceInstanceId(serviceInstanceId);

        for(BackupPlan plan : plansToDelete) {
            backupPlanService.deletePlan(plan.getId());
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
  
    @ApiOperation(value = "Update BackupPlan, running task and deletes old backup files & jobs exceeding the " +
            "retention period specified in the BackupPlan.")
    @RequestMapping(value = "/backupPlans/{planId}", method = RequestMethod.PATCH)
    public ResponseEntity<BackupPlan> update(@PathVariable() ObjectId planId, @Valid @RequestBody BackupPlan plan)
          throws BackupException {
        BackupPlan response = backupPlanService.updatePlan(planId, plan);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
