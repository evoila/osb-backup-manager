package de.evoila.cf.backup.queue;

import de.evoila.cf.backup.config.MessagingConfiguration;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class JobQueueConfigurer {

    private ConnectionFactory connectionFactory;
    private RabbitTemplate template;
    private JobMessageListener jobMessageLister;
    private AmqpAdmin amqpAdmin;
    private MessagingConfiguration config;

    public JobQueueConfigurer(ConnectionFactory factory, JobMessageListener jobMessageListener,
                              MessagingConfiguration config, AmqpAdmin amqpAdmin) {
        this.connectionFactory = factory;
        this.jobMessageLister = jobMessageListener;
        this.config = config;
        this.amqpAdmin = amqpAdmin;
    }

    @Bean
    public SimpleMessageListenerContainer jobMessageContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueues(jobQueue());
        container.setMessageListener(jobMessageLister);
        container.setAutoStartup(true);
        container.setConcurrentConsumers(2);
        container.setAcknowledgeMode(AcknowledgeMode.NONE);
        return container;
    }

    @Bean
    public Queue jobQueue() {
        Queue queue = new Queue(config.getQueue(), true);
        amqpAdmin.declareQueue(queue);
        return queue;
    }

    @Bean
    public DirectExchange jobDirectExchange() {
        DirectExchange directExchange = new DirectExchange(config.getExchange(), true, false);
        amqpAdmin.declareExchange(directExchange);
        return directExchange;
    }

    @Bean
    public Binding jobBinding() {
        Binding b = BindingBuilder.bind(jobQueue()).to(jobDirectExchange()).with(config.getRoutingKey());
        this.amqpAdmin.declareBinding(b);
        return b;
    }

}