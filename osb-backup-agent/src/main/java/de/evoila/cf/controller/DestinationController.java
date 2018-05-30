package de.evoila.cf.controller;

import de.evoila.cf.model.FileDestination;
import de.evoila.cf.openstack.OSException;
import de.evoila.cf.openstack.SwiftClient;
import de.evoila.cf.repository.FileDestinationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.util.List;

@Controller
public class DestinationController extends BaseController {

    @Autowired
    FileDestinationRepository desinationRepository;

    @RequestMapping(value = "/destinations/{destId}", method = RequestMethod.GET)
    public ResponseEntity<FileDestination> getDestinationUpdate(@PathVariable String destId) {
        FileDestination job = desinationRepository.findOne(destId);
        return new ResponseEntity<FileDestination>(job, HttpStatus.OK);
    }

    @RequestMapping("/destinations/byInstance/{instance}")
    public ResponseEntity<Page<FileDestination>> getByInstance(@PathVariable String instance,
                                                               @PageableDefault(size = 50, page = 0) Pageable pageable) {
        List<FileDestination> dest = desinationRepository.findByInstanceId(instance, pageable);
        Page<FileDestination> page = new PageImpl<FileDestination>(dest);
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @RequestMapping(value = "/destinations/{jobid}", method = RequestMethod.DELETE)
    public ResponseEntity deleteDestination(@PathVariable String jobid) {
        FileDestination job = desinationRepository.findOne(jobid);
        if (job == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
        desinationRepository.delete(job);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/destinations", method = RequestMethod.POST)
    public ResponseEntity<FileDestination> createDestination(@RequestBody FileDestination dest) {
        setName(dest);
        FileDestination response = desinationRepository.save(dest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/destinations/{destId}", method = RequestMethod.PUT)
    public ResponseEntity<FileDestination> updateDestination(@PathVariable() String destId,
                                                  @RequestBody FileDestination dest) {
        setName(dest);
        desinationRepository.delete(destId);
        desinationRepository.save(dest);
        return new ResponseEntity<>(dest, HttpStatus.OK);
    }

    @RequestMapping(value = "/destinations/validate", method = RequestMethod.POST)
    public ResponseEntity<FileDestination> updateDestination(@RequestBody FileDestination dest) {
        try {
            SwiftClient client = new SwiftClient(dest.getAuthUrl(),dest.getUsername(),dest.getPassword(),dest.getDomain(),dest.getProjectName());
            return new ResponseEntity<>(dest, HttpStatus.OK);
        } catch (OSException|IOException e) {
            return new ResponseEntity<>(dest, HttpStatus.BAD_REQUEST);
        }

    }

    public void setName (FileDestination dest) {
        if(dest.getName() == null){
            dest.setName(String.format("%s - %s - %s", dest.getDomain(), dest.getProjectName(), dest.getContainerName()));
        }
    }
}
