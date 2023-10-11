package de.evoila.cf;

import de.evoila.cf.backup.config.BaseAuthenticationConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

@Configuration
@Order(1)
public class BasicAuthSecurityConfiguration {

    @Autowired
    private BaseAuthenticationConfiguration authentication;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    InMemoryUserDetailsManager inMemoryAuthManager() throws Exception {
        return new InMemoryUserDetailsManager(User.builder().username(authentication.getUsername()).password(authentication.getPassword()).build());
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.securityMatchers(matchers -> matchers
                .requestMatchers(HttpMethod.DELETE, "/**/byInstance/**"))
                .httpBasic(withDefaults())
                .authorizeHttpRequests((authz) -> authz
                .anyRequest()
                .authenticated())
                .anonymous(AbstractHttpConfigurer::disable)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint()))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean (name = "basicAuthenticationEntryPoint")
    public AuthenticationEntryPoint authenticationEntryPoint() {
        BasicAuthenticationEntryPoint entryPoint =
                new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName("defaultEndpointRealm");
        return entryPoint;
    }
}
