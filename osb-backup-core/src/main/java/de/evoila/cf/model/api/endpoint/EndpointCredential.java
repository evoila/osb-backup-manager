package de.evoila.cf.model.api.endpoint;

import de.evoila.cf.model.ServiceInstance;
import de.evoila.cf.model.enums.BackupType;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.util.Map;

public class EndpointCredential {

    @DBRef
    private ServiceInstance serviceInstance;

    private String host;

    private int port;

    private String username;

    private String password;

    private String database;

    private BackupType type;

    private Map<String, Object> parameters;

    public ServiceInstance getServiceInstance() {
        return serviceInstance;
    }

    public void setServiceInstance(ServiceInstance serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public BackupType getType() {
        return type;
    }

    public void setType(BackupType type) {
        this.type = type;
    }

    public void setTypeFromString(String type) {
        this.type = BackupType.valueOf(type);
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
