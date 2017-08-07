package de.evoila.cf.model;

import de.evoila.cf.openstack.OSException;
import de.evoila.cf.repository.BackupPlanRepository;
import de.evoila.cf.service.BackupServiceManager;
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
        StreamSupport.stream(repository.findAll().spliterator(),true).forEach(plan -> addTask(plan));
    }

    @Bean(destroyMethod = "shutdown")
    public Executor taskExecutor () {
        return Executors.newScheduledThreadPool(10);
    }

    public void addTask (BackupPlan job) {
        Trigger trigger = new CronTrigger(job.getFrequency());
        BackupTask task = new BackupTask(job);
        ScheduledFuture scheduledFuture = threadPoolTaskScheduler().schedule(task, trigger);
        task.setScheduledFuture(scheduledFuture);
    }

    private class BackupTask implements Runnable{
        BackupPlan plan;
        ScheduledFuture scheduledFuture;

        public void setScheduledFuture (ScheduledFuture scheduledFuture) {
            this.scheduledFuture = scheduledFuture;
        }

        public BackupTask (BackupPlan plan) {
            this.plan = plan;
        }

        @Override
        public void run () {
            if(!repository.exists(plan.getId())) {
                if(scheduledFuture != null){
                    scheduledFuture.cancel(true);
                }
                return;
            }
            String frequency = plan.getFrequency();
            this.plan = repository.findOne(plan.getId());
            if(!frequency.equals(plan.getFrequency())){
                addTask(plan);
                scheduledFuture.cancel(true);
            }
            try {
                BackupJob job = backupServiceManager.backup(plan.getSource(), plan.getDestination());

                plan.getJobIds().add(job.getId());
                repository.save(plan);

                backupServiceManager.removeOldBackupFiles(plan);
                repository.save(plan);
            } catch (BackupRequestException e) {
                logger.error("Could not execute scheduled Backup " + e.getMessage());
            } catch (IOException e) {
                logger.warn("Could remove old Backup Files" + e.getMessage());
            } catch (OSException e) {
                logger.warn("Could remove old Backup Files" + e.getMessage());
            } finally {

            }
        }
    }

}
