package de.evoila.cf.backup.service;


import de.evoila.cf.backup.config.MessagingConfiguration;
import de.evoila.cf.backup.repository.BackupPlanRepository;
import de.evoila.cf.model.BackupPlan;
import de.evoila.cf.model.BackupRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.StreamSupport;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Service
@EnableScheduling
public class BackupSchedulingService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private BackupPlanRepository backupPlanRepository;

    private RabbitTemplate rabbitTemplate;

    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    private MessagingConfiguration messagingConfiguration;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

    public BackupSchedulingService(BackupPlanRepository backupPlanRepository,
                                   RabbitTemplate rabbitTemplate,
                                   MessagingConfiguration messagingConfiguration) {
        this.backupPlanRepository = backupPlanRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        this.messagingConfiguration = messagingConfiguration;
    }

    public ThreadPoolTaskScheduler threadPoolTaskScheduler () {
        return this.threadPoolTaskScheduler;
    }

    @PostConstruct
    private void init(){
        threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix("BackupThreadPoolTaskScheduler");
        threadPoolTaskScheduler().initialize();

        StreamSupport.stream(backupPlanRepository.findAll()
                .spliterator(),true)
                .forEach(plan -> addTask(plan));
    }

    @Bean(destroyMethod = "shutdown")
    public Executor taskExecutor() {
        return Executors
                .newScheduledThreadPool(10);
    }

    public void addTask(BackupPlan job) {
        log.debug(String.format("Starting Plan [%s] frequency:", job.getId(), job.getFrequency()));
        BackupTask task = new BackupTask(job);
        ScheduledFuture scheduledFuture = threadPoolTaskScheduler()
                .schedule(task, new CronTrigger(job.getFrequency()));
        scheduledTasks.put(job.getId(), scheduledFuture);

    }

    public void removeTask(BackupPlan job) {
        log.debug(String.format("Removing Plan [%s] frequency:", job.getId(), job.getFrequency()));
        ScheduledFuture scheduledFuture = scheduledTasks.get(job.getId());
        if (scheduledFuture != null)
            scheduledFuture.cancel(false);
    }

    public void updateTask(BackupPlan job) {
        log.debug(String.format("Updating Plan [%s] frequency:", job.getId(), job.getFrequency()));
        this.removeTask(job);
        this.addTask(job);
    }

    private class BackupTask implements Runnable {
        BackupPlan plan;

        public BackupTask(BackupPlan plan) {
            this.plan = plan;
        }

        @Override
        public void run() {
            log.debug(String.format("Scheduling Plan [%s] frequency:", plan.getId(), plan.getFrequency()));

            BackupRequest backupRequest = new BackupRequest();
            backupRequest.setDestinationId(plan.getDestinationId());
            backupRequest.setPlan(plan);

            rabbitTemplate.convertAndSend(messagingConfiguration.getExchange(),
                    messagingConfiguration .getRoutingKey(),
                    backupRequest);
        }
    }

}
