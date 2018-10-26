package de.evoila.cf.backup.repository;

import de.evoila.cf.model.FileDestination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FileDestinationRepository extends MongoRepository<FileDestination, String> {

    Page<FileDestination> findByInstanceId(String instanceId, Pageable pageable);

}
