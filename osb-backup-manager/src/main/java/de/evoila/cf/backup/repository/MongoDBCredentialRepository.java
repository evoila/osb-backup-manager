package de.evoila.cf.backup.repository;

import de.evoila.cf.broker.model.credential.Credential;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author Johannes Hiemer.
 *
 * A MongoDB repository for storing service instance credentials.
 * TODO this interface is only used in CredentialRepositoryImpl, which isn't used anywhere
 *
 */
public interface MongoDBCredentialRepository extends MongoRepository<Credential, String> {}
