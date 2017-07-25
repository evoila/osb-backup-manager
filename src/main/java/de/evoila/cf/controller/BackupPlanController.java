package de.evoila.cf.controller;

import de.evoila.cf.controller.exception.BackupException;
import de.evoila.cf.model.BackupPlan;
import de.evoila.cf.services.BackupPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * Created by yremmet on 19.07.17.
 */
@Controller
@RequestMapping(value = "/v2/dashboard/manage/backup")
public class BackupPlanController {

    @Autowired
    BackupPlanService backupPlanService;

    @RequestMapping(value = "/{serviceInstanceId}/plans", method = RequestMethod.GET)
    public ResponseEntity<List<BackupPlan>> getPlans (@PathVariable() String serviceInstanceId)
          throws BackupException{

        List<BackupPlan> response = backupPlanService.getPlans(serviceInstanceId);
        return new ResponseEntity<List<BackupPlan>>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "/{serviceInstanceId}/plans", method = RequestMethod.POST)
    public ResponseEntity<BackupPlan> createPlan (@PathVariable() String serviceInstanceId,
                                                  @RequestBody BackupPlan plan)
          throws BackupException{

        BackupPlan response = backupPlanService.createPlan(plan);
        return new ResponseEntity<BackupPlan>(response, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{serviceInstanceId}/plan/{planId}", method = RequestMethod.DELETE)
    public ResponseEntity<BackupPlan> deletePlan (@PathVariable() String serviceInstanceId,
                                                  @PathVariable() String planId)
          throws BackupException{

        BackupPlan response = backupPlanService.deletePlan(planId);
        return new ResponseEntity<BackupPlan>(response, HttpStatus.OK);
    }

    @RequestMapping(value = "a/{serviceInstanceId}/plan/{planId}", method = RequestMethod.PUT)
    public ResponseEntity<BackupPlan> updatePlan (@PathVariable() String serviceInstanceId,
                                                  @PathVariable() String planId,
                                                  @RequestBody BackupPlan plan)
          throws BackupException{

        BackupPlan response = backupPlanService.updatePlan(planId, plan);
        return new ResponseEntity<BackupPlan>(response, HttpStatus.OK);
    }



}
