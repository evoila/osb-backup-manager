package de.evoila.cf;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.issuer-uri")
public class UaaSecurityConfiguration {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.securityMatchers(matchers -> matchers
                .requestMatchers("/**"))
                .cors(withDefaults())
                .authorizeHttpRequests((authz) -> authz
                .anyRequest()
                .authenticated())
                .oauth2ResourceServer(server -> server
                        .jwt(withDefaults()));
        return http.build();
    }
}