package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.config.MessagingConfiguration;
import de.evoila.cf.model.BackupRequest;
import de.evoila.cf.model.RestoreRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
@Controller
public class BackupController {

    private final Logger log = LoggerFactory.getLogger(BackupController.class);

    private RabbitTemplate rabbitTemplate;

    private MessagingConfiguration messagingConfiguration;

    public BackupController(RabbitTemplate rabbitTemplate,
                            MessagingConfiguration messagingConfiguration) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingConfiguration = messagingConfiguration;
    }

	@RequestMapping(value = "/backup", method = RequestMethod.POST)
	public ResponseEntity backup(@RequestBody BackupRequest backupRequest) {
        rabbitTemplate.convertAndSend(messagingConfiguration.getExchange(),
                messagingConfiguration .getRoutingKey(),
                backupRequest);
		return new ResponseEntity<>(backupRequest, HttpStatus.CREATED);
	}

	@RequestMapping(value = "/restore", method = RequestMethod.POST)
	public ResponseEntity restore(@RequestBody RestoreRequest restoreRequest) {
        rabbitTemplate.convertAndSend(messagingConfiguration.getExchange(),
                messagingConfiguration .getRoutingKey(),
                restoreRequest);
		return new ResponseEntity<>(restoreRequest, HttpStatus.CREATED);
	}
}
