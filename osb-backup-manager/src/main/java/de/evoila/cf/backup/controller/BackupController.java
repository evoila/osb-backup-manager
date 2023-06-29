package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.config.MessagingConfiguration;
import de.evoila.cf.backup.service.permissions.PermissionCheckService;
import de.evoila.cf.model.api.request.BackupRequest;
import de.evoila.cf.model.api.request.RestoreRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 *
 * Proxies requests to RabbitMQ.
 */
@Api(value = "/",
        description = "Create a new request in the job repository (RabbitMQ), which can be either a backup job by " +
                "sending a BackupPlan or a restore job. Jobs are scheduled to be processed by this Backup-Manager.")
@Controller
public class BackupController {

    private final Logger log = LoggerFactory.getLogger(BackupController.class);

    private RabbitTemplate rabbitTemplate;

    private MessagingConfiguration messagingConfiguration;

    private PermissionCheckService permissionCheckService;

    public BackupController(RabbitTemplate rabbitTemplate,
                            MessagingConfiguration messagingConfiguration, PermissionCheckService permissionCheckService) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingConfiguration = messagingConfiguration;
        this.permissionCheckService = permissionCheckService;

    }

    @ApiOperation(value = "Add a new backup request to the queue. The backup will be executed asynchronously at the " +
            "scheduled interval as a job.")
    @RequestMapping(value = "/backup", method = RequestMethod.POST)
    public ResponseEntity backup(@RequestBody BackupRequest backupRequest) {

        String instanceID = backupRequest.getBackupPlan().getServiceInstance().getId();
        if (!permissionCheckService.hasReadAccess(instanceID)) {
            throw new AuthenticationServiceException("User is not authorised to access the requested resource. Please contact your System Administrator.");
        }

        rabbitTemplate.convertAndSend(messagingConfiguration.getExchange(),
                messagingConfiguration.getRoutingKey(),
                backupRequest);
        return new ResponseEntity(backupRequest, HttpStatus.CREATED);
    }

    @ApiOperation(value = "Add a new restore request to the queue. The restore will be executed asynchronously at " +
            "the scheduled interval as a job.")
    @RequestMapping(value = "/restore", method = RequestMethod.POST)
    public ResponseEntity restore(@RequestBody RestoreRequest restoreRequest) {

        String instanceID = restoreRequest.getBackupJob().getBackupPlan().getServiceInstance().getId();
        if (!permissionCheckService.hasReadAccess(instanceID)) {
            throw new AuthenticationServiceException("User is not authorised to access the requested resource. Please contact your System Administrator.");
        }

        rabbitTemplate.convertAndSend(messagingConfiguration.getExchange(),
                messagingConfiguration.getRoutingKey(),
                restoreRequest);
        return new ResponseEntity(restoreRequest, HttpStatus.CREATED);
    }
}
