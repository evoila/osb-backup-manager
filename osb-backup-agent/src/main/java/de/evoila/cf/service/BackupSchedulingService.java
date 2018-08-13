package de.evoila.cf.service;


import de.evoila.cf.controller.exception.BackupException;
import de.evoila.cf.model.BackupJob;
import de.evoila.cf.model.BackupPlan;
import de.evoila.cf.model.FileDestination;
import de.evoila.cf.model.enums.JobStatus;
import de.evoila.cf.openstack.OSException;
import de.evoila.cf.repository.BackupPlanRepository;
import de.evoila.cf.repository.FileDestinationRepository;
import de.evoila.cf.service.exception.BackupRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.StreamSupport;

/**
 * Created by yremmet on 19.07.17.
 */
@Service
@EnableScheduling
public class BackupSchedulingService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    BackupServiceManager backupServiceManager;

    @Autowired
    BackupPlanRepository repository;

    @Autowired
    FileDestinationRepository destinationRepository;

    ThreadPoolTaskScheduler threadPoolTaskScheduler;

    public ThreadPoolTaskScheduler threadPoolTaskScheduler () {
        return this.threadPoolTaskScheduler;
    }

    @PostConstruct
    private void init(){
        threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix("BackupThreadPoolTaskScheduler");
        threadPoolTaskScheduler().initialize();
        StreamSupport.stream(repository.findAll().spliterator(),true).forEach(plan -> addTask(plan));
    }

    @Bean(destroyMethod = "shutdown")
    public Executor taskExecutor() {
        return Executors.newScheduledThreadPool(10);
    }

    public void addTask(BackupPlan job) {
        logger.debug(String.format("Starting Plan [%s] frequency:", job.getId(), job.getFrequency()));
        Trigger trigger = new CronTrigger(job.getFrequency());
        BackupTask task = new BackupTask(job);
        ScheduledFuture scheduledFuture = threadPoolTaskScheduler().schedule(task, trigger);
        task.setScheduledFuture(scheduledFuture);
    }

    private class BackupTask implements Runnable {
        BackupPlan plan;
        ScheduledFuture scheduledFuture;

        public void setScheduledFuture(ScheduledFuture scheduledFuture) {
            this.scheduledFuture = scheduledFuture;
        }

        public BackupTask(BackupPlan plan) {
            this.plan = plan;
        }

        @Override
        public void run() {
            if (!repository.findById(plan.getId()).isPresent()) {
                if (scheduledFuture != null) {
                    scheduledFuture.cancel(true);
                }
                return;
            }

            String frequency = plan.getFrequency();
            if (!frequency.equals(plan.getFrequency())) {
                addTask(plan);
                scheduledFuture.cancel(true);
            }

            BackupJob job = null;
            try {
                FileDestination destination = destinationRepository.findById(plan.getDestinationId())..orElse(null)
                if (destination == null) {
                    throw new BackupException("Destination can not be found");
                }
                job = backupServiceManager.backup(plan, destination);
                plan.getJobIds().add(job.getId());
                repository.save(plan);
            } catch (BackupException | BackupRequestException ex) {
                String msg = String.format("Could not execute scheduled backup [Plan %s] : %s", plan.getId(), ex.getMessage());
                if (job != null) {
                    job.setStatus(JobStatus.FAILED);
                    job.appendLog(msg);
                }
                logger.error(msg, ex);
            } finally {
                try {
                    backupServiceManager.removeOldBackupFiles(plan);
                } catch(IOException | OSException ex) {
                    logger.error("Could not remove execute removeOldBackupFiles", ex);
                }
                repository.save(plan);
            }
        }
    }

}
