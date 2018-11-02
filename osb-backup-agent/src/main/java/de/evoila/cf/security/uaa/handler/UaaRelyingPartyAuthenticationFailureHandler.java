package de.evoila.cf.security.uaa.handler;

import de.evoila.cf.security.uaa.utils.DefaultCorsHeader;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** @author Johannes Hiemer. */
public class UaaRelyingPartyAuthenticationFailureHandler implements AuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
										AuthenticationException exception) throws IOException {

		// Needs to be made configurable in the Service Brokers
		response.addHeader(DefaultCorsHeader.WWW_AUTHENTICATE, "MeshFed realm="
				+ DefaultCorsHeader.getBaseUrl(request)
				+ "/federated-auth/login");

		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
	}

}