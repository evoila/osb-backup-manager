package de.evoila.cf.backup.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="cloudfoundry")
@ConditionalOnProperty(prefix="cloudfoundry", value="api")
public class CloudFoundryConfiguration {

    private String api;

    public CloudFoundryConfiguration() {
        System.out.println("CloudFoundryConfig");
    }

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }
}
