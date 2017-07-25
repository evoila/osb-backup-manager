package de.evoila.cf.model;

import de.evoila.cf.model.enums.DatabaseType;

public class DatabaseCredential {

    int port;
    String hostname;
    String username;
    String password;
    String context;
    DatabaseType type;

    public int getPort() {
        return port;
    }

    public String getHostname() {
        return hostname;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getContext() {
        return context;
    }

    public DatabaseType getType() {
        return type;
    }
}