package de.evoila.cf.backup.service;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.repository.BackupPlanRepository;
import de.evoila.cf.backup.repository.FileDestinationRepository;
import de.evoila.cf.model.BackupPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Service
public class BackupPlanService {

    BackupPlanRepository backupPlanRepository;

    BackupSchedulingService backupSchedulingService;

    FileDestinationRepository fileDestinationRepository;

    public BackupPlanService(BackupPlanRepository backupPlanRepository,
                             BackupSchedulingService backupSchedulingService,
                             FileDestinationRepository fileDestinationRepository) {
        this.backupPlanRepository = backupPlanRepository;
        this.backupSchedulingService = backupSchedulingService;
        this.fileDestinationRepository = fileDestinationRepository;
    }

    public Page<BackupPlan> getPlans(String serviceInstanceId, Pageable pageable) {
        return backupPlanRepository.findByServiceInstanceId(serviceInstanceId, pageable);
    }

    public BackupPlan createPlan(BackupPlan plan) throws BackupException {
        if(plan.getDestinationId() != null && !fileDestinationRepository.findById(plan.getDestinationId()).isPresent())
            throw new BackupException("Backup Destination does not exists ID = " + plan.getId());

        try {
            plan = backupPlanRepository.save(plan);
            backupSchedulingService.addTask(plan);
        } catch (Exception e){
            backupPlanRepository.delete(plan);
            throw new BackupException("Could not create Plan with");
        }
        return plan;
    }

    public BackupPlan deletePlan(String planId) {
        BackupPlan plan = backupPlanRepository.findById(planId).orElse(null);
        backupSchedulingService.removeTask(plan);
        backupPlanRepository.deleteById(planId);
        return plan;
    }

    public BackupPlan updatePlan(String planId, BackupPlan plan) throws BackupException {
        BackupPlan backupPlan = backupPlanRepository.findById(planId).orElse(null);

        if(backupPlan == null)
            throw new BackupException("Backup plan not found" + planId);
        if(fileDestinationRepository.findById(plan.getId()).isPresent())
            throw new BackupException("Backup Destination does not exists ID = " + plan.getId());

        backupPlan.update(plan);
        backupSchedulingService.updateTask(backupPlan);
        backupPlanRepository.save(backupPlan);
        return plan;
    }

    public BackupPlan getPlan(String planId) {
        return backupPlanRepository.findById(planId).orElse(null);
    }
}
