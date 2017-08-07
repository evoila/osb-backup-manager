package de.evoila.cf.repository;


import de.evoila.cf.model.BackupPlan;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Created by yremmet on 18.07.17.
 */
public interface BackupPlanRepository extends MongoRepository<BackupPlan, String> {

    List<BackupPlan> findBySourceContext (String serviceInstanceId, Pageable pageable);
}
