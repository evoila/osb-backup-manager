package de.evoila.cf.backup.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.service.exception.BackupRequestException;
import de.evoila.cf.backup.service.manager.BackupServiceManager;
import de.evoila.cf.backup.service.manager.RestoreServiceManager;
import de.evoila.cf.model.api.request.BackupRequest;
import de.evoila.cf.model.api.request.RestoreRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.stereotype.Service;

@Service
public class JobMessageListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(JobMessageListener.class);

    private Jackson2JsonMessageConverter messageConverter;

    private BackupServiceManager backupServiceManager;

    private RestoreServiceManager restoreServiceManager;

    public JobMessageListener(BackupServiceManager backupServiceManager,
                              RestoreServiceManager restoreServiceManager) {
        this.backupServiceManager = backupServiceManager;
        this.restoreServiceManager = restoreServiceManager;

        messageConverter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("de.evoila.cf.model.api.request");
        classMapper.setDefaultType(BackupRequest.class);
        messageConverter.setClassMapper(classMapper);
    }

    @Override
    public void onMessage(Message message) {
        Object request =  messageConverter.fromMessage(message);
        try {
            if (request instanceof BackupRequest) {
                handleMessage((BackupRequest) request);
            } else if (request instanceof RestoreRequest) {
                handleMessage((RestoreRequest) request);
            }
        } catch (BackupRequestException | BackupException e){
            log.error("Could not execute backup Request: " + e.getMessage());
        }
    }

    private void handleMessage(BackupRequest backupRequest) throws BackupRequestException, BackupException {
        try {
            log.info(
                    new ObjectMapper()
                            .writer()
                            .withDefaultPrettyPrinter()
                            .writeValueAsString(backupRequest));
        } catch(Exception ex) {
            throw new BackupRequestException("Failed to deserialize JSON object");
        }
        backupServiceManager.backup(backupRequest);
    }

    private void handleMessage(RestoreRequest restoreRequest) throws BackupRequestException {
        try {
            log.info(
                    new ObjectMapper()
                            .writer()
                            .withDefaultPrettyPrinter()
                            .writeValueAsString(restoreRequest));
        } catch(Exception ex) {
            throw new BackupRequestException("Failed to deserialize JSON object");
        }

        restoreServiceManager.restore(restoreRequest);
    }
}
