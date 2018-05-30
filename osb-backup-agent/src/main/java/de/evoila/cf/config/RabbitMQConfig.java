package de.evoila.cf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "queue")
public class RabbitMQConfig {

    private String exchange;
    private String routingKey;
    private String queue;

    public String getExchange () {
        return exchange;
    }

    public void setExchange (String exchange) {
        this.exchange = exchange;
    }

    public String getRoutingKey () {
        return routingKey;
    }

    public void setRoutingKey (String routingKey) {
        this.routingKey = routingKey;
    }

    public String getQueue () {
        return queue;
    }

    public void setQueue (String queue) {
        this.queue = queue;
    }
}
