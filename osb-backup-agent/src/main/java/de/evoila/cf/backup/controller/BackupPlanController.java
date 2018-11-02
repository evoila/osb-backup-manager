package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.service.BackupPlanService;
import de.evoila.cf.model.BackupPlan;
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
public class BackupPlanController {

    BackupPlanService backupPlanService;

    public BackupPlanController(BackupPlanService backupPlanService) {
        this.backupPlanService = backupPlanService;
    }

    @RequestMapping(value = "/plans/byInstance/{instanceId}", method = RequestMethod.GET)
    public ResponseEntity<Page<BackupPlan>> all(@PathVariable() String instanceId,
                                                      @PageableDefault(size = 50, page = 0) Pageable pageable) {
        Page<BackupPlan> response = backupPlanService.getPlans(instanceId, pageable);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/plans", method = RequestMethod.POST)
    public ResponseEntity<BackupPlan> create(@RequestBody BackupPlan plan) throws BackupException {

        BackupPlan response = backupPlanService.createPlan(plan);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/plans/{planId}", method = RequestMethod.GET)
    public ResponseEntity<BackupPlan> get(@PathVariable() String planId) {

        BackupPlan response = backupPlanService.getPlan(planId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/plans/{planId}", method = RequestMethod.DELETE)
    public ResponseEntity<BackupPlan> delete(@PathVariable() String planId) {

        BackupPlan response = backupPlanService.deletePlan(planId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/plans/{planId}", method = RequestMethod.PUT)
    public ResponseEntity<BackupPlan> update(@PathVariable() String planId, @RequestBody BackupPlan plan)
          throws BackupException {

        BackupPlan response = backupPlanService.updatePlan(planId, plan);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
