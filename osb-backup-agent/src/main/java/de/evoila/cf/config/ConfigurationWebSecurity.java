package de.evoila.cf.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@Configuration
@EnableWebSecurity
public class ConfigurationWebSecurity extends WebSecurityConfigurerAdapter {
    private final static Logger log = LoggerFactory.getLogger(ConfigurationWebSecurity.class);
    private AuthenticationProperties authentication;

    ConfigurationWebSecurity(AuthenticationProperties ap) {
        super();
        authentication = ap;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Autowired
    protected void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        log.info(String.format("User : %s, Password: %s", authentication.getUsername(), authentication.getPassword()));
        auth.inMemoryAuthentication()
              .withUser(authentication.getUsername())
              .password(authentication.getPassword())
              .roles(authentication.getRole());
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
              .antMatcher("/**")
              .authorizeRequests()
              .antMatchers("/**").hasRole("USER")
              .and()
              .httpBasic()
        ;
        ;
    }
}
