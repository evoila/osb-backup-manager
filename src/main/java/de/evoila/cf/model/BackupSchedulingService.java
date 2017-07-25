package de.evoila.cf.model;

import de.evoila.cf.repository.BackupAgentJobRepository;
import de.evoila.cf.repository.BackupPlanRepository;
import de.evoila.cf.service.BackupServiceManager;
import de.evoila.cf.service.exception.BackupRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

    ThreadPoolTaskScheduler threadPoolTaskScheduler;

    public ThreadPoolTaskScheduler threadPoolTaskScheduler () {
        return this.threadPoolTaskScheduler;
    }

    @PostConstruct
    private void init(){
        threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix( "BackupThreadPoolTaskScheduler");
        threadPoolTaskScheduler().initialize();
        repository.findAll().stream().parallel().forEach(plan -> addTask(plan));
    }

    @Bean(destroyMethod = "shutdown")
    public Executor taskExecutor () {
        return Executors.newScheduledThreadPool(10);
    }

    public void addTask (BackupPlan job) {
        Trigger trigger = new CronTrigger(job.getFrequency());
        threadPoolTaskScheduler().schedule(new BackupTask(job), trigger);
    }

    private class BackupTask implements Runnable{
        BackupPlan plan;
        public BackupTask (BackupPlan plan) {
            this.plan = plan;
        }

        @Override
        public void run () {
            if(!repository.exists(plan.getId())) {
                return;
            }
            try {
                BackupJob job = backupServiceManager.backup(plan.getSource(), plan.getDestination());

                plan.getJobIds().add(job.getId());
                repository.save(plan);

                backupServiceManager.removeOldBackupFiles(plan);
                // TODO: remove old files from swift
            } catch (BackupRequestException e) {
                logger.error("Could not execute scheduled Backup " + e.getMessage());
            } finally {
                if(repository.exists(plan.getId())) {
                    addTask(plan);
                }
            }
        }
    }

}
