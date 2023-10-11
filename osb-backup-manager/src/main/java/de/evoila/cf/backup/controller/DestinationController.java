package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.clients.S3Client;
import de.evoila.cf.backup.clients.SwiftClient;
import de.evoila.cf.backup.repository.BackupPlanRepository;
import de.evoila.cf.backup.repository.FileDestinationRepository;
import de.evoila.cf.backup.service.permissions.PermissionCheckService;
import de.evoila.cf.model.api.file.FileDestination;
import de.evoila.cf.model.api.file.S3FileDestination;
import de.evoila.cf.model.api.file.SwiftFileDestination;
import de.evoila.cf.model.enums.DestinationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Tag(name = "/fileDestinations", description = "Manage where backup files should be stored by defining destinations.")
@Controller
public class DestinationController extends BaseController {

    Logger log = LoggerFactory.getLogger(DestinationController.class);

    FileDestinationRepository destinationRepository;

    BackupPlanRepository backupPlanRepository;

    PermissionCheckService permissionCheckService;

    public DestinationController(FileDestinationRepository destinationRepository, BackupPlanRepository backupPlanRepository, PermissionCheckService permissionCheckService) {
        this.destinationRepository = destinationRepository;
        this.backupPlanRepository = backupPlanRepository;
        this.permissionCheckService = permissionCheckService;
    }

    @Operation(summary = "Get the specified destination from the repository.")
    @GetMapping("/fileDestinations/{destinationId}")
    public ResponseEntity<FileDestination> get(@PathVariable ObjectId destinationId) {
        FileDestination job = destinationRepository.findById(destinationId).orElse(null);
        return new ResponseEntity<>(job, HttpStatus.OK);
    }

    @Operation(summary = "Get a page of destinations configured on the given instance.")
    @GetMapping("/fileDestinations/byInstance/{serviceInstanceId}")
    public ResponseEntity<Page<FileDestination>> all(@PathVariable String serviceInstanceId,
                                                     @PageableDefault(size = 50, page = 0) Pageable pageable) {
        Page<FileDestination> dest = destinationRepository.findByServiceInstanceId(serviceInstanceId, pageable);
        return new ResponseEntity<>(dest, HttpStatus.OK);
    }

    @Operation(summary = "Delete a destination with the given ID.")
    @DeleteMapping("/fileDestinations/{destinationId}")
    public ResponseEntity delete(@PathVariable ObjectId destinationId) {
        FileDestination fileDestination = destinationRepository.findById(destinationId).orElse(null);
        if (fileDestination == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        if(!isDestinationDeletable(fileDestination)) {
            return new ResponseEntity(HttpStatus.CONFLICT);
        }
        destinationRepository.delete(fileDestination);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "Delete all destinations configured on the given instance.")
    @DeleteMapping("/fileDestinations/byInstance/{serviceInstanceId}")
    public ResponseEntity deleteByInstance(@PathVariable String serviceInstanceId) {
        destinationRepository.deleteByServiceInstanceId(serviceInstanceId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Operation(summary = """
            Create a new S3 destination, specifying where backups \
            should be stored for a specific instance.\
            """)
    @PostMapping("/fileDestinations")
    public ResponseEntity<FileDestination> create(@RequestBody FileDestination destination) {

        String instanceID = destination.getServiceInstance().getId();
        if (!permissionCheckService.hasReadAccess(instanceID)) {
            throw new AuthenticationServiceException("User is not authorised to access the requested resource. Please contact your System Administrator.");
        }

        S3FileDestination s3FileDestination = (S3FileDestination) destination;
        s3FileDestination.evaluateSkipSSL();
        FileDestination response = destinationRepository.save(s3FileDestination);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update an existing destination with new configurations.")
    @PatchMapping("/fileDestinations/{destinationId}")
    public ResponseEntity<FileDestination> update(@PathVariable() ObjectId destinationId,
                                                  @RequestBody FileDestination destination) {
        S3FileDestination s3FileDestination = (S3FileDestination) destination;
        s3FileDestination.evaluateSkipSSL();
        destinationRepository.save(s3FileDestination);
        return new ResponseEntity<>(destination, HttpStatus.OK);
    }

    @Operation(summary = "Check if a backup can be stored in the given destination.")
    @PostMapping("/fileDestinations/validate")
    public ResponseEntity validate(@RequestBody FileDestination destination) {

        if (destination == null || destination.getServiceInstance() == null || !permissionCheckService.hasReadAccess(destination.getServiceInstance().getId())) {
            throw new AuthenticationServiceException("User is not authorised to access the requested resource. Please contact your System Administrator.");
        }

        try {
            if (destination.getType().equals(DestinationType.SWIFT)) {
                SwiftFileDestination swiftFileDestination = (SwiftFileDestination) destination;
                new SwiftClient(swiftFileDestination.getAuthUrl(), swiftFileDestination.getUsername(),
                        swiftFileDestination.getPassword(), swiftFileDestination.getDomain(), swiftFileDestination.getProjectName());
            } else if (destination.getType().equals(DestinationType.S3)) {
                S3FileDestination s3FileDestination = (S3FileDestination) destination;
                S3Client s3client = new S3Client(s3FileDestination.getEndpoint(), s3FileDestination.getRegion(), s3FileDestination.getAuthKey(),
                        s3FileDestination.getAuthSecret(), destinationRepository);
                //Simply creating a client won't throw an exception in case the data is false. Therefore we need an explicit validation for writing data
                s3client.validate(s3FileDestination);
            }
            return new ResponseEntity<>(destination, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Could not validate Endpoint. " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

    }

    /**
     * Checks if a file destination that should be deleted is still used in one or more plans.
     * @param destination The destination the user wants to delete
     * @return True if the destination is not used in any plans, false if it is
     */
    private boolean isDestinationDeletable(FileDestination destination) {
        if(backupPlanRepository.findByFileDestinationId(destination.getId()).isEmpty())
            return true;

        return false;
    }
}
