package de.evoila.cf.backup.config;

import de.evoila.cf.backup.Interceptors.ServiceInstancePermissionsInterceptor;
import de.evoila.cf.backup.service.PermissionCheckServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
@ConditionalOnBean(CloudFoundryConfiguration.class)
public class PermissionsInterceptorConfiguration implements WebMvcConfigurer {

    private Logger log = LoggerFactory.getLogger(PermissionsInterceptorConfiguration.class);

    @Autowired
    private PermissionCheckServiceImpl permissionsCheckService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Adding ServiceInstancePermissionsInterceptor.");
        registry.addInterceptor(new ServiceInstancePermissionsInterceptor(permissionsCheckService)).addPathPatterns(
                "/*/byInstance/**",
                "/backupJobs/**",
                "/backupPlans/**",
                "/fileDestinations/**",
                "/restoreJobs/**"
        ).excludePathPatterns("/fileDestinations/validate");
    }
}
