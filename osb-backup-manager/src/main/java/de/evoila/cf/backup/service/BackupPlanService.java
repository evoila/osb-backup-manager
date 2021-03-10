package de.evoila.cf.backup.service;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.repository.BackupPlanRepository;
import de.evoila.cf.backup.repository.FileDestinationRepository;
import de.evoila.cf.backup.repository.ServiceInstanceRepository;
import de.evoila.cf.model.api.BackupPlan;
import org.bson.types.ObjectId;
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

    ServiceInstanceRepository serviceInstanceRepository;

    public BackupPlanService(BackupPlanRepository backupPlanRepository,
                             BackupSchedulingService backupSchedulingService,
                             FileDestinationRepository fileDestinationRepository,
                             ServiceInstanceRepository serviceInstanceRepository) {
        this.backupPlanRepository = backupPlanRepository;
        this.backupSchedulingService = backupSchedulingService;
        this.fileDestinationRepository = fileDestinationRepository;
        this.serviceInstanceRepository = serviceInstanceRepository;
    }

    public Page<BackupPlan> getPlans(String serviceInstanceId, Pageable pageable) {
        return backupPlanRepository.findByServiceInstanceId(serviceInstanceId, pageable);
    }

    public BackupPlan createPlan(BackupPlan backupPlan) throws BackupException {
        if(backupPlan.getFileDestination() == null)
            throw new BackupException("Backup Destination does not exists ID = " + backupPlan.getId());

        try {
            backupPlan = backupPlanRepository.save(backupPlan);
            if (!backupPlan.isPaused())
                backupSchedulingService.addTask(backupPlan);
        } catch (Exception ex) {
            backupPlanRepository.delete(backupPlan);
            throw new BackupException("Could not create Plan", ex);
        }
        return backupPlan;
    }

    public BackupPlan updatePlan(ObjectId planId, BackupPlan plan) throws BackupException {
        BackupPlan backupPlan = backupPlanRepository.findById(planId)
                .orElse(null);

        if(backupPlan == null)
            throw new BackupException("Backup plan not found" + planId);
        if(!fileDestinationRepository.findById(plan.getFileDestination().getId()).isPresent())
            throw new BackupException("Backup Destination does not exists ID = " + plan.getFileDestination().getId());

        try {
            backupPlan.update(plan);
            if (backupPlan.isPaused())
                backupSchedulingService.removeTask(backupPlan);
            else
                backupSchedulingService.updateTask(backupPlan);
            backupPlanRepository.save(backupPlan);
        } catch (Exception ex) {
            throw new BackupException("Could not update plan", ex);
        }

        return plan;
    }

    public BackupPlan deletePlan(ObjectId planId) {
        BackupPlan backupPlan = backupPlanRepository.findById(planId)
                .orElse(null);
        if (backupPlan != null) {
            backupSchedulingService.removeTask(backupPlan);
            backupPlanRepository.delete(backupPlan);
        }
        return backupPlan;
    }

    public BackupPlan getPlan(ObjectId planId) {
        return backupPlanRepository.findById(planId).orElse(null);
    }

}
