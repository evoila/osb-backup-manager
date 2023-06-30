package de.evoila.cf.backup.service.permissions;

import de.evoila.cf.UaaSecurityConfiguration;
import de.evoila.cf.backup.config.CloudFoundryConfiguration;
import de.evoila.cf.backup.config.PermissionsInterceptorConfiguration;
import de.evoila.cf.backup.repository.AbstractJobRepository;
import de.evoila.cf.backup.repository.BackupPlanRepository;
import de.evoila.cf.backup.repository.FileDestinationRepository;
import de.evoila.cf.model.api.AbstractJob;
import de.evoila.cf.model.api.BackupPlan;
import de.evoila.cf.model.api.file.FileDestination;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.NoSuchElementException;

@Component
@ConditionalOnBean(PermissionsInterceptorConfiguration.class)
public class PermissionCheckServiceImpl implements PermissionCheckService{

    private Logger log = LoggerFactory.getLogger(PermissionCheckServiceImpl.class);

    private static final String CF_PERMISSIONS_ENDPOINT = "/v2/service_instances/:guid/permissions";

    private static final String USER_ID = "user_id";
    private static final String SUB = "sub";
    private static final String READ = "read";
    private static final String MANAGE = "manage";

    @Autowired
    private CloudFoundryConfiguration cloudFoundryConfiguration;

    @Autowired
    private AbstractJobRepository abstractJobRepository;

    @Autowired
    private FileDestinationRepository fileDestinationRepository;

    @Autowired
    private BackupPlanRepository backupPlanRepository;

    public PermissionCheckServiceImpl(CloudFoundryConfiguration cloudFoundryConfiguration,
                                      AbstractJobRepository abstractJobRepository,
                                      FileDestinationRepository fileDestinationRepository,
                                      BackupPlanRepository backupPlanRepository) {
        log.info("Creating PermissionCheckServiceImpl to check user permissions on service instances.");
        this.cloudFoundryConfiguration = cloudFoundryConfiguration;
        this.abstractJobRepository = abstractJobRepository;
        this.fileDestinationRepository = fileDestinationRepository;
        this.backupPlanRepository = backupPlanRepository;
    }


    public boolean hasReadAccess(HttpServletRequest request) {
        try {
            return hasScope(request, READ);
        } catch (NoSuchElementException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean hasReadAccess(String serviceInstanceId){
        return hasScope(serviceInstanceId, READ);
    }

    public boolean hasScope(String serviceInstanceId, String requiredScope){
        if (serviceInstanceId != null) {
            return (boolean) fetchPermissions(serviceInstanceId).getBody().get(requiredScope);
        }
        return false;
    }

    public boolean hasScope(HttpServletRequest request, String requiredScope) throws NoSuchElementException {
        Map<Object, Object> attributes = (Map<Object, Object>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String serviceInstanceId = (String) attributes.get("serviceInstanceId");

        log.debug("Checking permissions with serviceInstanceId...");
        if (serviceInstanceId != null) {
            return (boolean) fetchPermissions(serviceInstanceId).getBody().get(requiredScope);
        }
        log.debug("ServiceInstanceId is null. Checking permissions with jobID...");

        String jobId = (String) attributes.get("jobId");
        if (jobId != null) {
            AbstractJob job = abstractJobRepository.findById(new ObjectId(jobId)).get();
            serviceInstanceId = job.getServiceInstance().getId();
            return serviceInstanceId != null && (boolean) fetchPermissions(serviceInstanceId).getBody().get(requiredScope);
        }
        log.debug("JobID is null. Checking permissions with destinationId...");

        String fileDestinationId = (String) attributes.get("destinationId");
        if (fileDestinationId != null) {
            FileDestination fileDestination = fileDestinationRepository.findById(new ObjectId(fileDestinationId)).get();
            serviceInstanceId = fileDestination.getServiceInstance().getId();
            return serviceInstanceId != null && (boolean) fetchPermissions(serviceInstanceId).getBody().get(requiredScope);
        }
        log.debug("FileDestinationId is null. Checking permissions with BackupPlanId...");

        String backupPlanId = (String) attributes.get("planId");
        if (backupPlanId != null) {
            BackupPlan backupPlan = backupPlanRepository.findById(new ObjectId(backupPlanId)).get();
            serviceInstanceId = backupPlan.getServiceInstance().getId();
            return serviceInstanceId != null && (boolean) fetchPermissions(serviceInstanceId).getBody().get(requiredScope);
        }
        log.debug("BackupPlan is null. No more IDs to get ServiceInstance from. No access allowed.");

        return false;
    }

    private ResponseEntity<Map> fetchPermissions(String serviceInstanceId) {
        HttpEntity<String> httpEntity = new HttpEntity<>(getAuthHeader());
        String uri = CF_PERMISSIONS_ENDPOINT.replace(":guid", serviceInstanceId);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(cloudFoundryConfiguration.getApi()));

        ResponseEntity<Map> response = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, Map.class);

        if (response.getStatusCode().isError()) {
            System.out.println(response.getBody());
            throw new AuthenticationServiceException("Failed to request permissions for " + serviceInstanceId + ".");
        }

        return response;
    }

    private HttpHeaders getAuthHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + getUserToken());
        return headers;
    }

    private String getUserToken() {
        System.out.println("SecurityContext = " + SecurityContextHolder.getContext().toString());
        System.out.println("SecurityContext = " + SecurityContextHolder.getContext().getAuthentication().toString());

        JwtAuthenticationToken jwtAuthenticationToken = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        System.out.println("jwtAuthenticationToken = " + jwtAuthenticationToken.toString());
        System.out.println("Token = " + jwtAuthenticationToken.getToken().toString());
        System.out.println("TokenValue = " + jwtAuthenticationToken.getToken().getTokenValue().toString());
        return jwtAuthenticationToken.getToken().getTokenValue();
    }


}
