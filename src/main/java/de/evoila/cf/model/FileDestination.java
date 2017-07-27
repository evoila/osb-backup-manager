package de.evoila.cf.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.evoila.cf.model.enums.DestinationType;

public class FileDestination implements SwiftConfig{

    private String authUrl;
    private String username;
    private String password;
    private String domain;
    private String containerName;
    private String projectName;
    private DestinationType type;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String filename;

    public String getFilename () {
        return filename;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDomain () {
        return domain;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getProjectName () {
        return projectName;
    }

    public DestinationType getType() {
        return type;
    }

    public void setFilename (String filename) {
        this.filename = filename;
    }

    public void setAuthUrl (String authUrl) {
        this.authUrl = authUrl;
    }

    public void setUsername (String username) {
        this.username = username;
    }
    public void setPassword (String password) {
        this.password = password;
    }

    public void setDomain (String domain) {
        this.domain = domain;
    }

    public void setContainerName (String containerName) {
        this.containerName = containerName;
    }

    public void setProjectName (String projectName) {
        this.projectName = projectName;
    }

    public void setType (DestinationType type) {
        this.type = type;
    }
}