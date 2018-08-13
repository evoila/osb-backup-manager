package de.evoila.cf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.evoila.cf.controller.exception.BackupException;
import de.evoila.cf.model.BackupRequest;
import de.evoila.cf.model.RestoreRequest;
import de.evoila.cf.service.exception.BackupRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JobMessageListener implements MessageListener{

    private static final Logger log = LoggerFactory.getLogger(JobMessageListener.class);

    @Autowired
    BackupServiceManager service;

    Jackson2JsonMessageConverter messageConverter;

    public JobMessageListener(){
        messageConverter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setDefaultType(BackupRequest.class);
        messageConverter.setClassMapper(classMapper);
    }

    @Override
    public void onMessage (Message message) {
        Object request =  messageConverter.fromMessage(message);
        try {
            if (request instanceof BackupRequest) {
                handleMessage((BackupRequest) request);
            } else if (request instanceof RestoreRequest) {
                handleMessage((RestoreRequest) request);
            }
        } catch (BackupRequestException | BackupException e){
            log.error("Couldnd´t execute backup Request" + e.getMessage());
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

        }
        service.backup(backupRequest);
    }

    private void handleMessage(RestoreRequest request) throws BackupRequestException {
        service.restore(request);
    }
}
