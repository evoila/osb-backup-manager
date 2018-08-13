package de.evoila.cf.service;

import de.evoila.cf.controller.exception.BackupException;
import de.evoila.cf.model.BackupPlan;
import de.evoila.cf.repository.BackupPlanRepository;
import de.evoila.cf.repository.FileDestinationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class BackupPlanService {

    @Autowired
    BackupPlanRepository repository;

    @Autowired
    BackupSchedulingService backupSchedulingService;

    @Autowired
    FileDestinationRepository destinationRepository;

    public Page<BackupPlan> getPlans(String serviceInstanceId, Pageable pageable) {
        return repository.findByServiceInstanceId(serviceInstanceId, pageable);
    }

    public BackupPlan createPlan(BackupPlan plan) throws BackupException {
        if(plan.getDestinationId() != null && !destinationRepository.findById(plan.getDestinationId()).isPresent()){
            throw new BackupException("Backup Destination does not exists ID = " + plan.getId());
        }
        try {
            plan = repository.save(plan);
            backupSchedulingService.addTask(plan);
        } catch (Exception e){
            repository.delete(plan);
            throw new BackupException("Could not create Plan with");
        }
        return plan;
    }

    public BackupPlan deletePlan(String planId) {
        BackupPlan plan = repository.findById(planId).orElse(null);
        repository.deleteById(planId);
        return plan;
    }

    public BackupPlan updatePlan(String planId, BackupPlan plan) throws BackupException {
        BackupPlan backupPlan = repository.findById(planId).orElse(null);
        if(backupPlan == null)
            throw new BackupException("Backup plan not found" + planId);
        if(destinationRepository.findById(plan.getId()).isPresent()){
            throw new BackupException("Backup Destination does not exists ID = " + plan.getId());
        }
        backupPlan.update(plan);
        plan = repository.save(backupPlan);
        return plan;
    }

    public BackupPlan getPlan(String planId) {
        return repository.findById(planId).orElse(null);
    }
}
