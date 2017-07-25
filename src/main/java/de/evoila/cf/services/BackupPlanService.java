package de.evoila.cf.services;

import de.evoila.cf.controller.exception.BackupException;
import de.evoila.cf.model.BackupPlan;
import de.evoila.cf.model.BackupSchedulingService;
import de.evoila.cf.repository.BackupPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BackupPlanService {

    @Autowired
    BackupPlanRepository repository;
    @Autowired
    BackupSchedulingService backupSchedulingService;


    public List<BackupPlan> getPlans (String serviceInstanceId) {
        return repository.findBySourceContext(serviceInstanceId);
    }

    public BackupPlan createPlan (BackupPlan plan) throws BackupException {
        plan = repository.save(plan);
        backupSchedulingService.addTask(plan);
        return plan;
    }


    public BackupPlan deletePlan (String planId) {
        BackupPlan plan = repository.findOne(planId);
        repository.delete(planId);
        return plan;
    }

    public BackupPlan updatePlan (String planId, BackupPlan plan) {
        BackupPlan backupPlan = repository.findOne(planId);
        backupPlan.update(plan);
        plan = repository.save(backupPlan);
        return plan;
    }

}
