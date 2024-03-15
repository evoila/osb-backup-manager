package de.evoila.cf.backup.Interceptors;

import de.evoila.cf.backup.service.permissions.PermissionCheckServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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

        String methodAndUri = request.getMethod() + " " + request.getRequestURI().toString();
        log.debug("Starting preHandle for '" + methodAndUri + "'.");

        // In these cases the check for read access is not needed or applicable
        if ((handler instanceof ResourceHttpRequestHandler ||
                handler instanceof ParameterizableViewController)
            ||
                (SecurityContextHolder.getContext() != null &&
                        SecurityContextHolder.getContext().getAuthentication() instanceof UsernamePasswordAuthenticationToken)
            ||
            (!
                (request.getMethod().equals("GET") ||
                request.getMethod().equals("POST") ||
                request.getMethod().equals("PATCH") ||
                request.getMethod().equals("DELETE") ))
            ) {
            log.debug("Not intercepting on '" + methodAndUri + "' based on SecurityContext, HTTP Method or handler Class.");
            return true;
        }

        log.info("Intercepting on '" + methodAndUri + "'");
        if (!permissionsCheckService.hasReadAccess(request)) {
            log.info("Access to '" + methodAndUri + "'is not allowed.");
            throw new AuthenticationServiceException("User is not authorised to access '" + methodAndUri + "'. Please contact your System Administrator.");
        }
        log.debug("Access to '" + methodAndUri + "'is allowed.");
        return true;
    }


}
