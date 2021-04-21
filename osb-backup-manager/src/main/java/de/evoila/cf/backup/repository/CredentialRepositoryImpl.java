package de.evoila.cf.backup.repository;

import de.evoila.cf.broker.model.credential.Credential;
import de.evoila.cf.broker.repository.CredentialRepository;
import org.springframework.stereotype.Service;

/**
 * @author Johannes Hiemer.
 *
 * A MongoDB repository for storing credentials of service instances?
 * TODO this class isn't used anywhere
 *
 */
@Service
public class CredentialRepositoryImpl implements CredentialRepository {

    private MongoDBCredentialRepository mongoDBCredentialRepository;

    public CredentialRepositoryImpl(MongoDBCredentialRepository mongoDBCredentialRepository) {
        this.mongoDBCredentialRepository = mongoDBCredentialRepository;
    }

    @Override
    public void save(Credential credential) {
        this.mongoDBCredentialRepository.save(credential);
    }

    @Override
    public Credential getById(String identifier) {
        return this.mongoDBCredentialRepository.findById(identifier).get();
    }

    @Override
    public void deleteById(String identifier) {
        this.mongoDBCredentialRepository.deleteById(identifier);
    }

    @Override
    public void delete(Credential credential) {
        this.mongoDBCredentialRepository.delete(credential);
    }
}
