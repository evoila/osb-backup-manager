package de.evoila.cf.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import de.evoila.cf.model.api.endpoint.ServerAddress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Johannes Hiemer.
 */
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class ServiceInstance implements BaseEntity<String> {

	@JsonSerialize
	@JsonProperty("service_instance_id")
	private String id;

	@JsonSerialize
	@JsonProperty("service_id")
	private String serviceDefinitionId;

	@JsonSerialize
	@JsonProperty("plan_id")
	private String planId;

	@JsonSerialize
	@JsonProperty("organization_guid")
	private String organizationGuid;

	@JsonSerialize
	@JsonProperty("space_guid")
	private String spaceGuid;

	@JsonIgnore
	private String dashboardUrl;

	@JsonSerialize
	@JsonProperty("parameters")
	private Map<String, Object> parameters = new HashMap<>();

	@JsonSerialize
	@JsonProperty("internal_id")
	private String internalId;

	@JsonSerialize
	@JsonProperty("hosts")
	private List<ServerAddress> hosts = new ArrayList<>();
	
	@JsonSerialize
	@JsonProperty("context")
	private Map<String, String> context;

	@JsonSerialize
	@JsonProperty("floatingIp_id")
	private String floatingIpId;

    @JsonIgnore
    private String username;

    @JsonIgnore
    private String password;

    @JsonIgnore
    private String usergroup;

	@Override
	public String getId() {
		return id;
	}

	private void setId(String id) {
		this.id = id;
	}

	public String getServiceDefinitionId() {
		return serviceDefinitionId;
	}

	private void setServiceDefinitionId(String serviceDefinitionId) {
		this.serviceDefinitionId = serviceDefinitionId;
	}

	public String getPlanId() {
		return planId;
	}

	private void setPlanId(String planId) {
		this.planId = planId;
	}
	
	public void updatePlanId(String planId){
		this.setPlanId(planId);
	}

	public String getOrganizationGuid() {
		return organizationGuid;
	}

	private void setOrganizationGuid(String organizationGuid) {
		this.organizationGuid = organizationGuid;
	}

	public String getSpaceGuid() {
		return spaceGuid;
	}

	private void setSpaceGuid(String spaceGuid) {
		this.spaceGuid = spaceGuid;
	}

	public String getDashboardUrl() {
		return dashboardUrl;
	}

	private void setDashboardUrl(String dashboardUrl) {
		this.dashboardUrl = dashboardUrl;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	private void setParameters(Map<String, Object> parameters) {
		this.parameters = new HashMap<>(parameters);
	}

	public String getInternalId() {
		return internalId;
	}

	private void setInternalId(String internalId) {
		this.internalId = internalId;
	}

	public List<ServerAddress> getHosts() {
		return hosts;
	}

	public void setHosts(List<ServerAddress> hosts) {
		this.hosts = hosts;
	}

	public Map<String, String> getContext() {
		return context;
	}

	public void setContext(Map<String, String> context) {
		this.context = new HashMap<String, String>(context);
	}

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

    public String getUsergroup() { return usergroup; }

    public void setUsergroup(String usergroup) { this.usergroup = usergroup; }

	public String getFloatingIpId() { return floatingIpId; }

	public void setFloatingIpId(String floatingIpId) { this.floatingIpId = floatingIpId; }
}
