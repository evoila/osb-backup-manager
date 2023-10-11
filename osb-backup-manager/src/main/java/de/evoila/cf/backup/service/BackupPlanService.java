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
 *
 * The BackupPlanService provides methods to create, read, update and delete BackupPlans from the repository. When
 * the repository is modified, the BackupPlanService also modifies the associated tasks accordingly. Tasks are used
 * to add Backup- and RestoreRequests to the queue, so that they can be further processed as jobs.
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

    /**
     * Gets a page of BackupPlans from the repository configured on the given instance.
     *
     * @param serviceInstanceId ID of the service instance, for which to create backups.
     * @param pageable How many entries per page should be returned.
     * @return A page of BackupPlans associated with the instance.
     */
    public Page<BackupPlan> getPlans(String serviceInstanceId, Pageable pageable) {
        return backupPlanRepository.findByServiceInstanceId(serviceInstanceId, pageable);
    }

    /**
     * Adds the given BackupPlan to the repository. A destination needs to be configured for the instance. When
     * the BackupPlan has been successfully added to the repository, a task will be created to periodically
     * add BackupRequest to the queue.
     *
     * @param backupPlan Configurations for how and what backups should be created
     * @return The created BackupPlan
     * @throws BackupException
     */
    public BackupPlan createPlan(BackupPlan backupPlan) throws BackupException {
        if(fileDestinationRepository.findById(backupPlan.getFileDestination().getId()).isEmpty())
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

    /**
     * Update an existing BackupPlan in the repository and associated tasks. Task will periodically add
     * BackupRequest to the queue.
     *
     * @param planId ID of the BackupPlan
     * @param plan The new BackupPlan to be used as a replacement
     * @return The new BackupPlan
     * @throws BackupException
     */
    public BackupPlan updatePlan(ObjectId planId, BackupPlan plan) throws BackupException {
        BackupPlan backupPlan = backupPlanRepository.findById(planId)
                .orElse(null);

        if(backupPlan == null)
            throw new BackupException("Backup plan not found" + planId);
        if(fileDestinationRepository.findById(backupPlan.getFileDestination().getId()).isEmpty())
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

    /**
     * Remove a BackupPlan from the repository and delete all associated tasks.
     *
     * @param planId ID of the BackupPlan
     * @return The removed BackupPlan
     */
    public BackupPlan deletePlan(ObjectId planId) {
        BackupPlan backupPlan = backupPlanRepository.findById(planId)
                .orElse(null);
        if (backupPlan != null) {
            backupSchedulingService.removeTask(backupPlan);
            backupPlanRepository.delete(backupPlan);
        }
        return backupPlan;
    }

    /**
     * Gets a single BackupPlan from the repository matching the given ID.
     *
     * @param planId ID of the BackupPlan
     * @return The BackupPlan or null
     */
    public BackupPlan getPlan(ObjectId planId) {
        return backupPlanRepository.findById(planId).orElse(null);
    }

}
