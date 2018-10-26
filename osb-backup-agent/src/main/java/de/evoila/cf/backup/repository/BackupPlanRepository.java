package de.evoila.cf.backup.repository;


import de.evoila.cf.model.BackupPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Created by yremmet on 18.07.17.
 */
public interface BackupPlanRepository extends MongoRepository<BackupPlan, String> {

    Page<BackupPlan> findByServiceInstanceId(String serviceInstanceId, Pageable pageable);
}
