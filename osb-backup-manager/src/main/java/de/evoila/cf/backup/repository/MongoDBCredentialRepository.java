package de.evoila.cf.backup.repository;

import de.evoila.cf.broker.model.credential.Credential;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author Johannes Hiemer.
 */
public interface MongoDBCredentialRepository extends MongoRepository<Credential, String> {}
