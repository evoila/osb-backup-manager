package de.evoila.cf.repository;

import de.evoila.cf.model.FileDestination;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FileDestinationRepository extends MongoRepository<FileDestination, String> {
    List<FileDestination> findByInstanceId (String instanceId, Pageable pageable);

}
