package de.evoila.cf.backup.service;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.repository.BackupPlanRepository;
import de.evoila.cf.backup.repository.FileDestinationRepository;
import de.evoila.cf.backup.service.manager.BackupServiceManager;
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

    BackupServiceManager backupServiceManager;

    public BackupPlanService(BackupPlanRepository backupPlanRepository,
                             BackupSchedulingService backupSchedulingService,
                             FileDestinationRepository fileDestinationRepository,
                             BackupServiceManager backupServiceManager) {
        this.backupPlanRepository = backupPlanRepository;
        this.backupSchedulingService = backupSchedulingService;
        this.fileDestinationRepository = fileDestinationRepository;
        this.backupServiceManager = backupServiceManager;
    }

    public Page<BackupPlan> getPlans(String serviceInstanceId, Pageable pageable) {
        return backupPlanRepository.findByServiceInstanceId(serviceInstanceId, pageable);
    }

    public BackupPlan createPlan(BackupPlan backupPlan) throws BackupException {
        if(!fileDestinationRepository.findById(backupPlan.getFileDestination().getId()).isPresent())
            throw new BackupException("Could not create Plan. Backup Destination does not exists ID = " +
                    backupPlan.getFileDestination().getId());
        if(backupPlan.getRetentionPeriod() <= 0) {
            throw new BackupException("Could not create Plan. Invalid retention value \"" +
                    backupPlan.getRetentionPeriod() + "\". Value must be greater than 0");
        }
        if(backupPlanRepository.findByNameAndServiceInstanceId(backupPlan.getName(),
                backupPlan.getServiceInstance().getId()) != null) {
            throw new BackupException("Could not create plan. Backup Plan with name " + backupPlan.getName() +
                    " already exists");
        }

        try {
            backupPlan = backupPlanRepository.save(backupPlan);
            backupSchedulingService.checkIfFrequencyIsValid(backupPlan);
            if (!backupPlan.isPaused())
                backupSchedulingService.addTask(backupPlan);
        } catch (Exception ex) {
            backupPlanRepository.delete(backupPlan);
            throw new BackupException("Could not create Plan. " + ex.getMessage());
        }
        return backupPlan;
    }

    public BackupPlan updatePlan(ObjectId planId, BackupPlan plan) throws BackupException {
        BackupPlan backupPlan = backupPlanRepository.findById(planId)
                .orElse(null);

        if(backupPlan == null)
            throw new BackupException("Backup plan not found" + planId);
        if(!fileDestinationRepository.findById(backupPlan.getFileDestination().getId()).isPresent())
            throw new BackupException("Could not update Plan. Backup Destination does not exists ID = " +
                    backupPlan.getFileDestination().getId());
        if(backupPlan.getRetentionPeriod() <= 0) {
            throw new BackupException("Could not update Plan. Invalid retention value \"" +
                    backupPlan.getRetentionPeriod() + "\". Value must be greater than 0");
        }

        BackupPlan checkPlan = backupPlanRepository.findByNameAndServiceInstanceId(backupPlan.getName(),
                backupPlan.getServiceInstance().getId());

        if(checkPlan != null && !plan.getId().equals(checkPlan.getId())) {
            throw new BackupException("Could not update plan. Backup Plan with name " + backupPlan.getName() +
                    " already exists");
        }

        try {
            backupPlan.update(plan);
            backupSchedulingService.checkIfFrequencyIsValid(backupPlan);
            if (backupPlan.isPaused())
                backupSchedulingService.removeTask(backupPlan);
            else
                backupSchedulingService.updateTask(backupPlan);
            backupServiceManager.deleteIfDataRetentionIsReached(backupPlan);
            backupPlanRepository.save(backupPlan);
        } catch (Exception ex) {
            throw new BackupException("Could not update plan. " + ex.getMessage());
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
