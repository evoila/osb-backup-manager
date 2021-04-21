package de.evoila.cf.backup.repository;

import de.evoila.cf.model.api.AbstractJob;
import de.evoila.cf.model.api.BackupPlan;
import de.evoila.cf.model.enums.JobStatus;
import de.evoila.cf.model.enums.JobType;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * @author Yanic Remmet, Johannes Hiemer.
 *
 * A MongoDB repository for storing ongoing or finished jobs of different types.
 */
public interface AbstractJobRepository extends MongoRepository<AbstractJob, ObjectId> {

    Page<AbstractJob> findByServiceInstanceIdAndJobType(String serviceInstanceId,
                                                     JobType type, Pageable pageable);

    Page<AbstractJob> findByServiceInstanceIdAndJobTypeAndStatus(String serviceInstanceId,
                                                                    JobType type, JobStatus status, Pageable pageable);

    List<AbstractJob> findByBackupPlan(BackupPlan plan);

    List<AbstractJob> deleteByServiceInstanceId(String serviceInstanceId);

}
