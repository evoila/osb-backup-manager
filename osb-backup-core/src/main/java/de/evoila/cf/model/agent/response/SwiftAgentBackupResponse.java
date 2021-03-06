package de.evoila.cf.model.agent.response;

/**
 * @author Johannes Hiemer.
 */
public class SwiftAgentBackupResponse extends AgentBackupResponse {

    private String domain;

    private String containerName;

    private String projectName;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getContainerName() {
        return containerName;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
