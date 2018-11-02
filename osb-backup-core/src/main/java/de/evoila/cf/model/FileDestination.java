package de.evoila.cf.model;

import com.fasterxml.jackson.annotation.*;
import de.evoila.cf.model.enums.DestinationType;

/**
 * @author Yannic Remmet, Johannes Hiemer
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = S3FileDestination.class, name = "S3"),
        @JsonSubTypes.Type(value = SwiftFileDestination.class, name = "SWIFT")
})
public abstract class FileDestination {

    private String id;

    private String serviceInstanceId;

    private String name;

    private String username;

    private String password;

    private DestinationType type;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String filename;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getServiceInstanceId() {
        return serviceInstanceId;
    }

    public void setServiceInstanceId(String serviceInstanceId) {
        this.serviceInstanceId = serviceInstanceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @JsonProperty("password")
    public void setPassword(String password) {
        this.password = password;
    }

    public DestinationType getType() {
        return type;
    }

    public void setType(DestinationType type) {
        this.type = type;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

}