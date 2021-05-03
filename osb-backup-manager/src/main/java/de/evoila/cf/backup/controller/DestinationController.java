package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.clients.S3Client;
import de.evoila.cf.backup.clients.SwiftClient;
import de.evoila.cf.backup.repository.BackupPlanRepository;
import de.evoila.cf.backup.repository.FileDestinationRepository;
import de.evoila.cf.model.api.file.FileDestination;
import de.evoila.cf.model.api.file.S3FileDestination;
import de.evoila.cf.model.api.file.SwiftFileDestination;
import de.evoila.cf.model.enums.DestinationType;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class DestinationController extends BaseController {

    Logger log = LoggerFactory.getLogger(DestinationController.class);

    FileDestinationRepository destinationRepository;

    BackupPlanRepository backupPlanRepository;

    public DestinationController(FileDestinationRepository destinationRepository, BackupPlanRepository backupPlanRepository) {
        this.destinationRepository = destinationRepository;
        this.backupPlanRepository = backupPlanRepository;
    }

    @GetMapping(value = "/fileDestinations/{destinationId}")
    public ResponseEntity<FileDestination> get(@PathVariable ObjectId destinationId) {
        FileDestination job = destinationRepository.findById(destinationId).orElse(null);
        return new ResponseEntity<>(job, HttpStatus.OK);
    }

    @GetMapping("/fileDestinations/byInstance/{instanceId}")
    public ResponseEntity<Page<FileDestination>> all(@PathVariable String instanceId,
                                                     @PageableDefault(size = 50, page = 0) Pageable pageable) {
        Page<FileDestination> dest = destinationRepository.findByServiceInstanceId(instanceId, pageable);
        return new ResponseEntity<>(dest, HttpStatus.OK);
    }

    @DeleteMapping(value = "/fileDestinations/{destinationId}")
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

    @DeleteMapping(value = "/fileDestinations/byInstance/{serviceInstanceId}")
    public ResponseEntity deleteByInstance(@PathVariable String serviceInstanceId) {
        destinationRepository.deleteByServiceInstanceId(serviceInstanceId);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping(value = "/fileDestinations")
    public ResponseEntity<FileDestination> create(@RequestBody FileDestination destination) {
        S3FileDestination s3FileDestination = (S3FileDestination) destination;
        s3FileDestination.evaluateSkipSSL();
        FileDestination response = destinationRepository.save(s3FileDestination);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PatchMapping(value = "/fileDestinations/{destinationId}")
    public ResponseEntity<FileDestination> update(@PathVariable ObjectId destinationId,
                                                  @RequestBody FileDestination destination) {
        S3FileDestination s3FileDestination = (S3FileDestination) destination;
        s3FileDestination.evaluateSkipSSL();
        destinationRepository.save(s3FileDestination);
        return new ResponseEntity<>(destination, HttpStatus.OK);
    }

    @PostMapping(value = "/fileDestinations/validate")
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
