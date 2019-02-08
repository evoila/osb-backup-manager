/**
 * 
 */
package de.evoila.cf.model.api.endpoint;

/**
 * @author Christian Brinker, evoila.*
 */
public class ServerAddress {

    private String name;

    private String ip;

    private int port;

    private boolean backup;

    public ServerAddress() {
        super();
    }

    public ServerAddress(String name) {
        super();
        this.name = name;
    }

    public ServerAddress(String name, String ip) {
        super();
        this.name = name;
        this.ip = ip;
    }

    public ServerAddress(String name, String ip, int port) {
        super();
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    public ServerAddress(String name, String ip, int port, boolean backup) {
        super();
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.backup = backup;
    }

    public ServerAddress(ServerAddress address) {
        super();
        this.name = address.name;
        this.ip = address.ip;
        this.port = address.port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isBackup() {
        return backup;
    }

    public void setBackup(boolean backup) {
        this.backup = backup;
    }
}
