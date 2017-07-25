package de.evoila.cf.model;

import de.evoila.cf.model.enums.DestinationType;

public class FileDestination implements SwiftConfig{

    String authUrl;
    String username;
    String password;
    String domain;
    String containerName;
    String projectName;
    DestinationType type;
    private String filename;


    @Override
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
}