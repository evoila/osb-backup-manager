package de.evoila.cf.backup.controller;

import de.evoila.cf.backup.clients.S3Client;
import de.evoila.cf.backup.clients.SwiftClient;
import de.evoila.cf.backup.repository.FileDestinationRepository;
import de.evoila.cf.model.api.file.FileDestination;
import de.evoila.cf.model.api.file.S3FileDestination;
import de.evoila.cf.model.api.file.SwiftFileDestination;
import de.evoila.cf.model.enums.DestinationType;
import org.bson.types.ObjectId;
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

@Controller
public class DestinationController extends BaseController {

    FileDestinationRepository destinationRepository;

    public DestinationController(FileDestinationRepository destinationRepository) {
        this.destinationRepository = destinationRepository;
    }

    @RequestMapping(value = "/fileDestinations/{destinationId}", method = RequestMethod.GET)
    public ResponseEntity<FileDestination> get(@PathVariable ObjectId destinationId) {
        FileDestination job = destinationRepository.findById(destinationId).orElse(null);
        return new ResponseEntity<>(job, HttpStatus.OK);
    }

    @RequestMapping("/fileDestinations/byInstance/{instanceId}")
    public ResponseEntity<Page<FileDestination>> all(@PathVariable String instanceId,
                                                     @PageableDefault(size = 50, page = 0) Pageable pageable) {
        Page<FileDestination> dest = destinationRepository.findByServiceInstanceId(instanceId, pageable);
        return new ResponseEntity<>(dest, HttpStatus.OK);
    }

    @RequestMapping(value = "/fileDestinations/{destinationId}", method = RequestMethod.DELETE)
    public ResponseEntity delete(@PathVariable ObjectId destinationId) {
        FileDestination fileDestination = destinationRepository.findById(destinationId).orElse(null);
        if (fileDestination == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        destinationRepository.delete(fileDestination);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/fileDestinations", method = RequestMethod.POST)
    public ResponseEntity<FileDestination> create(@RequestBody FileDestination destination) {
        FileDestination response = destinationRepository.save(destination);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/fileDestinations/{destinationId}", method = RequestMethod.PATCH)
    public ResponseEntity<FileDestination> update(@PathVariable() ObjectId destinationId,
                                                  @RequestBody FileDestination destination) {
        destinationRepository.save(destination);
        return new ResponseEntity<>(destination, HttpStatus.OK);
    }

    @RequestMapping(value = "/fileDestinations/validate", method = RequestMethod.POST)
    public ResponseEntity<FileDestination> validate(@RequestBody FileDestination destination) {
        try {
            if (destination.getType().equals(DestinationType.SWIFT)) {
                SwiftFileDestination swiftFileDestination = (SwiftFileDestination) destination;
                new SwiftClient(swiftFileDestination.getAuthUrl(), swiftFileDestination.getUsername(),
                        swiftFileDestination.getPassword(), swiftFileDestination.getDomain(), swiftFileDestination.getProjectName());
            } else if (destination.getType().equals(DestinationType.S3)) {
                S3FileDestination s3FileDestination = (S3FileDestination) destination;
                new S3Client(s3FileDestination.getRegion(), s3FileDestination.getAuthKey(), s3FileDestination.getAuthSecret());
            }
            return new ResponseEntity<>(destination, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(destination, HttpStatus.BAD_REQUEST);
        }

    }
}
