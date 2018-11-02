package de.evoila.cf.backup.repository;

import de.evoila.cf.model.BackupJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Created by yremmet on 06.07.17.
 */

public interface  BackupAgentJobRepository extends MongoRepository<BackupJob, String> {

    Page<BackupJob> findByServiceInstanceId(String serviceInstanceId , Pageable pageable);
    List<BackupJob> findByServiceInstanceId(String serviceInstanceId);

}
