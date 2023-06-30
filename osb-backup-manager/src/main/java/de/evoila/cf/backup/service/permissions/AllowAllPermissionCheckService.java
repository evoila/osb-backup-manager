package de.evoila.cf.backup.service.permissions;

import de.evoila.cf.backup.config.CloudFoundryConfiguration;
import de.evoila.cf.backup.config.PermissionsInterceptorConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
@ConditionalOnMissingBean(PermissionsInterceptorConfiguration.class)
public class AllowAllPermissionCheckService implements PermissionCheckService {

    private Logger log = LoggerFactory.getLogger(AllowAllPermissionCheckService.class);

    public AllowAllPermissionCheckService() {
        log.info("Creating AllowAllPermissionCheckService.");
    }

    @Override
    public boolean hasReadAccess(HttpServletRequest request) {
        return true;
    }

    @Override
    public boolean hasReadAccess(String serviceInstanceId) {
        return true;
    }
}
