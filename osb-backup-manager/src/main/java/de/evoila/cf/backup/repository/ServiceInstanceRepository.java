package de.evoila.cf.backup.repository;

import de.evoila.cf.model.ServiceInstance;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ServiceInstanceRepository extends MongoRepository<ServiceInstance, String>  {

}