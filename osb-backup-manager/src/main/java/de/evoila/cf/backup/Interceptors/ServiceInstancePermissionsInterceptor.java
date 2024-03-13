package de.evoila.cf.backup.Interceptors;

import de.evoila.cf.backup.service.permissions.PermissionCheckServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.stream.Collectors;

public class ServiceInstancePermissionsInterceptor implements HandlerInterceptor {

    private Logger log = LoggerFactory.getLogger(ServiceInstancePermissionsInterceptor.class);

    private PermissionCheckServiceImpl permissionsCheckService;

    public ServiceInstancePermissionsInterceptor(PermissionCheckServiceImpl permissionsCheckService) {
        this.permissionsCheckService = permissionsCheckService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.debug("Debugging preHandle. Infos about request - AuthType: " + request.getAuthType() + ". Query: " + request.getQueryString() + ". Context path: " + request.getContextPath());
        Map<Object, Object> attributes = (Map<Object, Object>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String mapAsString = attributes.keySet().stream()
                .map(key -> key + "=" + attributes.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
        log.debug("Values: " + mapAsString);

        if ((handler instanceof ResourceHttpRequestHandler ||
                handler instanceof ParameterizableViewController)
            ||
            (!
                (request.getMethod().equals("GET") ||
                request.getMethod().equals("POST") ||
                request.getMethod().equals("PATCH") ||
                request.getMethod().equals("DELETE") ))
            ) {

            return true;
        }

        String methodAndUri = request.getMethod() + " " + request.getRequestURI().toString();
        log.info("Intercepting on '" + methodAndUri + "'");
        if (!permissionsCheckService.hasReadAccess(request)) {
            log.info("Access to '" + methodAndUri + "'is not allowed.");
            throw new AuthenticationServiceException("User is not authorised to access '" + methodAndUri + "'. Please contact your System Administrator.");
        }
        return true;
    }


}
