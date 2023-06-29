package de.evoila.cf.backup.Interceptors;

import de.evoila.cf.backup.service.permissions.PermissionCheckServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServiceInstancePermissionsInterceptor implements HandlerInterceptor {

    private Logger log = LoggerFactory.getLogger(ServiceInstancePermissionsInterceptor.class);

    private PermissionCheckServiceImpl permissionsCheckService;

    public ServiceInstancePermissionsInterceptor(PermissionCheckServiceImpl permissionsCheckService) {
        this.permissionsCheckService = permissionsCheckService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("Intercepting on URI '" + request.getRequestURI().toString() + "'");
        if (!permissionsCheckService.hasReadAccess(request)) {
            log.info("Access is not allowed.");
            throw new AuthenticationServiceException("User is not authorised to access the requested resource. Please contact your System Administrator.");
        }
        return true;
    }


}