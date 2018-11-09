package de.evoila.cf.backup.service;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.repository.ServiceInstanceRepository;
import de.evoila.cf.model.ServiceInstance;
import de.evoila.cf.model.api.endpoint.EndpointCredential;
import de.evoila.cf.model.enums.BackupType;
import org.springframework.stereotype.Component;

@Component
public class CredentialService {

    private ServiceInstanceRepository serviceInstanceRepository;

    public CredentialService(ServiceInstanceRepository serviceInstanceRepository) {
        this.serviceInstanceRepository = serviceInstanceRepository;
    }

    public EndpointCredential getCredentials(ServiceInstance serviceInstance) throws BackupException {
        ServiceInstance fullServiceInstance = serviceInstanceRepository
                .findById(serviceInstance.getId()).orElse(null);
        if(fullServiceInstance == null || fullServiceInstance.getHosts().size() <= 0) {
            throw new BackupException("Could not find Service Instance: " + serviceInstance.getId());
        }

        EndpointCredential credential = new EndpointCredential();
        credential.setServiceInstance(fullServiceInstance);
        credential.setUsername(fullServiceInstance.getUsername());
        credential.setPassword(fullServiceInstance.getPassword());
        credential.setHost(fullServiceInstance.getHosts().get(0).getIp());
        credential.setPort(fullServiceInstance.getHosts().get(0).getPort());
        credential.setType(BackupType.AGENT);

        return credential;
    }

}
