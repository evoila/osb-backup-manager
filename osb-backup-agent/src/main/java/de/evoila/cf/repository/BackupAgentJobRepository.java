package de.evoila.cf.repository;

import de.evoila.cf.model.BackupJob;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Created by yremmet on 06.07.17.
 */

public interface  BackupAgentJobRepository extends MongoRepository<BackupJob, String> {
  List<BackupJob> findByInstanceId (String instanceId , Pageable pageable);
  List<BackupJob> findByInstanceId (String instanceId);
}
