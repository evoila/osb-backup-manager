package de.evoila.cf.security.uaa.handler;

import de.evoila.cf.security.uaa.utils.DefaultCorsHeader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** @author Johannes Hiemer. */
public class CommonCorsAuthenticationEntryPoint implements AuthenticationEntryPoint,
        InitializingBean {

    private String realmName;

    public void afterPropertiesSet() throws Exception {
        Assert.hasText(realmName, "realmName must be specified");
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {

        response.addHeader(DefaultCorsHeader.WWW_AUTHENTICATE, "MeshFed realm="
                + DefaultCorsHeader.getBaseUrl(request)
                + "/federated-auth/login");
        response.sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase());
    }
    public String getRealmName() {
        return realmName;
    }

    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }
}