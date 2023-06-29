package de.evoila.cf.backup.service;

import de.evoila.cf.backup.config.CloudFoundryConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
@ConditionalOnMissingBean(CloudFoundryConfiguration.class)
public class AllowAllPermissionCheckService implements PermissionCheckService {

    @Override
    public boolean hasReadAccess(HttpServletRequest request) {
        return true;
    }

    @Override
    public boolean hasReadAccess(String serviceInstanceId) {
        return true;
    }
}
