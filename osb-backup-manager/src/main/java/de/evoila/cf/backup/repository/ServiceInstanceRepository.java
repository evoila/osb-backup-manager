package de.evoila.cf.backup.repository;

import de.evoila.cf.broker.model.ServiceInstance;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author Johannes Hiemer.
 */
public interface ServiceInstanceRepository extends MongoRepository<ServiceInstance, String>  {

}