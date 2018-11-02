package de.evoila.cf.backup.service;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.repository.BackupPlanRepository;
import de.evoila.cf.backup.repository.FileDestinationRepository;
import de.evoila.cf.backup.repository.ServiceInstanceRepository;
import de.evoila.cf.model.BackupPlan;
import de.evoila.cf.model.EndpointCredential;
import de.evoila.cf.model.ServiceInstance;
import de.evoila.cf.model.enums.BackupType;
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

    public BackupPlan createPlan(BackupPlan plan) throws BackupException {
        if(plan.getDestinationId() != null && !fileDestinationRepository.findById(plan.getDestinationId()).isPresent())
            throw new BackupException("Backup Destination does not exists ID = " + plan.getId());

        try {
            plan.setSource(getCredentials(plan.getServiceInstanceId()));
            plan = backupPlanRepository.save(plan);
            backupSchedulingService.addTask(plan);
        } catch (Exception ex) {
            backupPlanRepository.delete(plan);
            throw new BackupException("Could not create Plan", ex);
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

        try {
            plan.setSource(getCredentials(plan.getServiceInstanceId()));
            backupPlan.update(plan);
            backupSchedulingService.updateTask(backupPlan);
            backupPlanRepository.save(backupPlan);
        } catch (Exception ex) {
            throw new BackupException("Could not update plan", ex);
        }

        return plan;
    }

    public BackupPlan getPlan(String planId) {
        return backupPlanRepository.findById(planId).orElse(null);
    }

    public EndpointCredential getCredentials(String serviceInstanceId) throws BackupException {
        ServiceInstance instance = serviceInstanceRepository.findById(serviceInstanceId).orElse(null);

        if(instance == null || instance.getHosts().size() <= 0) {
            throw new BackupException("Could not find Service Instance: " + serviceInstanceId);
        }

        EndpointCredential credential = new EndpointCredential();
        credential.setServiceInstanceId(instance.getId());
        credential.setUsername(instance.getUsername());
        credential.setPassword(instance.getPassword());
        credential.setHostname(instance.getHosts().get(0).getIp());
        credential.setPort(instance.getHosts().get(0).getPort());
        credential.setType(BackupType.AGENT);

        return credential;
    }

}
