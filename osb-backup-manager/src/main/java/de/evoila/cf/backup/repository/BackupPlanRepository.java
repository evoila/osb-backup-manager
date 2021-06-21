package de.evoila.cf.backup.repository;


import de.evoila.cf.model.api.BackupPlan;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Created by yremmet on 18.07.17.
 *
 * A MongoDB repository for storing BackupPlans. A BackupPlan defines service instance, destinations, schedule and more.
 */
public interface BackupPlanRepository extends MongoRepository<BackupPlan, ObjectId> {

    Page<BackupPlan> findByServiceInstanceId(String serviceInstanceId, Pageable pageable);

    List<BackupPlan> findByFileDestinationId(ObjectId fileDestinationId);

    List<BackupPlan> findByServiceInstanceId(String serviceInstanceId);

    BackupPlan findByNameAndServiceInstanceId(String name, String serviceInstanceId);
}
