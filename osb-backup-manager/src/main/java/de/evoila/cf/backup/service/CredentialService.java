package de.evoila.cf.backup.service;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.repository.ServiceInstanceRepository;
import de.evoila.cf.model.ServiceInstance;
import de.evoila.cf.model.api.endpoint.EndpointCredential;
import de.evoila.cf.model.api.endpoint.ServerAddress;
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

        ServerAddress backupEndpoint = fullServiceInstance.getHosts().stream().filter(serverAddress -> {
            if (serverAddress.isBackup())
                return true;
            return false;
        }).findFirst().orElse(null);

        EndpointCredential credential = new EndpointCredential();
        if (backupEndpoint != null) {
            credential.setServiceInstance(fullServiceInstance);
            credential.setUsername(fullServiceInstance.getUsername());
            credential.setPassword(fullServiceInstance.getPassword());
            credential.setHost(backupEndpoint.getIp());
            credential.setPort(backupEndpoint.getPort());
            credential.setType(BackupType.AGENT);
        } else
            throw new BackupException("Could not find valid Backup Endpoint in Hosts of Service Instances");

        return credential;
    }

}
