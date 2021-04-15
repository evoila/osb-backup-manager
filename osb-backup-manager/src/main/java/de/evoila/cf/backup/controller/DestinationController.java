package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.clients.S3Client;
import de.evoila.cf.backup.clients.SwiftClient;
import de.evoila.cf.backup.repository.BackupPlanRepository;
import de.evoila.cf.backup.repository.FileDestinationRepository;
import de.evoila.cf.model.api.file.FileDestination;
import de.evoila.cf.model.api.file.S3FileDestination;
import de.evoila.cf.model.api.file.SwiftFileDestination;
import de.evoila.cf.model.enums.DestinationType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Api(value = "/fileDestinations", description = "Manage where backup files should be stored by defining destinations.")
@Controller
public class DestinationController extends BaseController {

    Logger log = LoggerFactory.getLogger(DestinationController.class);

    FileDestinationRepository destinationRepository;

    BackupPlanRepository backupPlanRepository;

    public DestinationController(FileDestinationRepository destinationRepository, BackupPlanRepository backupPlanRepository) {
        this.destinationRepository = destinationRepository;
        this.backupPlanRepository = backupPlanRepository;
    }

    @ApiOperation(value = "Get the specified destination from the repository.")
    @RequestMapping(value = "/fileDestinations/{destinationId}", method = RequestMethod.GET)
    public ResponseEntity<FileDestination> get(@PathVariable ObjectId destinationId) {
        FileDestination job = destinationRepository.findById(destinationId).orElse(null);
        return new ResponseEntity<>(job, HttpStatus.OK);
    }

    @ApiOperation(value = "Get a page of destinations configured on the given instance.")
    @RequestMapping("/fileDestinations/byInstance/{instanceId}")
    public ResponseEntity<Page<FileDestination>> all(@PathVariable String instanceId,
                                                     @PageableDefault(size = 50, page = 0) Pageable pageable) {
        Page<FileDestination> dest = destinationRepository.findByServiceInstanceId(instanceId, pageable);
        return new ResponseEntity<>(dest, HttpStatus.OK);
    }

    @ApiOperation(value = "Delete a destination with the given ID.")
    @RequestMapping(value = "/fileDestinations/{destinationId}", method = RequestMethod.DELETE)
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

    @ApiOperation(value = "Delete all destinations configured on the given instance.")
    @RequestMapping(value = "/fileDestinations/byInstance/{serviceInstanceId}", method = RequestMethod.DELETE)
    public ResponseEntity deleteByInstance(@PathVariable String serviceInstanceId) {
        destinationRepository.deleteByServiceInstanceId(serviceInstanceId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @ApiOperation(value = "Create a new S3 destination, specifying where backups " +
            "should be stored for a specific instance.")
    @RequestMapping(value = "/fileDestinations", method = RequestMethod.POST)
    public ResponseEntity<FileDestination> create(@RequestBody FileDestination destination) {
        S3FileDestination s3FileDestination = (S3FileDestination) destination;
        s3FileDestination.evaluateSkipSSL();
        FileDestination response = destinationRepository.save(s3FileDestination);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @ApiOperation(value = "Update an existing destination with new configurations.")
    @RequestMapping(value = "/fileDestinations/{destinationId}", method = RequestMethod.PATCH)
    public ResponseEntity<FileDestination> update(@PathVariable() ObjectId destinationId,
                                                  @RequestBody FileDestination destination) {
        S3FileDestination s3FileDestination = (S3FileDestination) destination;
        s3FileDestination.evaluateSkipSSL();
        destinationRepository.save(s3FileDestination);
        return new ResponseEntity<>(destination, HttpStatus.OK);
    }

    @ApiOperation(value = "Check if a backup can be stored in the given destination.")
    @RequestMapping(value = "/fileDestinations/validate", method = RequestMethod.POST)
    public ResponseEntity validate(@RequestBody FileDestination destination) {
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
