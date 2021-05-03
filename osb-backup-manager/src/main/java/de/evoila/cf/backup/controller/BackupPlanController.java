package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.repository.BackupPlanRepository;
import de.evoila.cf.backup.service.BackupPlanService;
import de.evoila.cf.model.api.BackupPlan;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Controller
public class BackupPlanController {

    BackupPlanService backupPlanService;

    BackupPlanRepository backupPlanRepository;

    public BackupPlanController(BackupPlanService backupPlanService, BackupPlanRepository backupPlanRepository) {
        this.backupPlanService = backupPlanService;
        this.backupPlanRepository = backupPlanRepository;
    }

    @GetMapping(value = "/backupPlans/byInstance/{instanceId}")
    public ResponseEntity<Page<BackupPlan>> all(@PathVariable() String instanceId,
                                                @PageableDefault(size = 50, page = 0) Pageable pageable) {
        Page<BackupPlan> response = backupPlanService.getPlans(instanceId, pageable);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(value = "/backupPlans")
    public ResponseEntity<BackupPlan> create(@Valid @RequestBody BackupPlan plan) throws BackupException {
        BackupPlan response = backupPlanService.createPlan(plan);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping(value = "/backupPlans/{planId}")
    public ResponseEntity<BackupPlan> get(@PathVariable ObjectId planId) {

        BackupPlan response = backupPlanService.getPlan(planId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping(value = "/backupPlans/{planId}")
    public ResponseEntity<BackupPlan> delete(@PathVariable ObjectId planId) {

        BackupPlan response = backupPlanService.deletePlan(planId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping(value = "/backupPlans/byInstance/{serviceInstanceId}")
    public ResponseEntity deleteByInstance(@PathVariable String serviceInstanceId) {
        List<BackupPlan> plansToDelete = backupPlanRepository.findByServiceInstanceId(serviceInstanceId);

        for(BackupPlan plan : plansToDelete) {
            backupPlanService.deletePlan(plan.getId());
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping(value = "/backupPlans/{planId}")
    public ResponseEntity<BackupPlan> update(@PathVariable ObjectId planId, @Valid @RequestBody BackupPlan plan)
          throws BackupException {

        BackupPlan response = backupPlanService.updatePlan(planId, plan);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
