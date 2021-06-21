package de.evoila.cf.backup.repository;

import de.evoila.cf.model.api.file.FileDestination;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * @author Yanic Remmet, Johannes Hiemer.
 *
 * A MongoDB repository for storing file storage (cloud storage) FileDestinations. Backup files are stored in a server
 * linked to with a FileDestination.
 */
public interface FileDestinationRepository extends MongoRepository<FileDestination, ObjectId> {

    Page<FileDestination> findByServiceInstanceId(String serviceInstanceId, Pageable pageable);

    List<FileDestination> deleteByServiceInstanceId(String serviceInstanceId);

    FileDestination findByNameAndServiceInstanceId(String name, String serviceInstanceId);
}
