package de.evoila.cf.service;

import de.evoila.cf.config.RabbitMQConfig;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class JobQueueConfigurer {

    private ConnectionFactory connectionFactory;
    private JobMessageListener jobMessageLister;
    private AmqpAdmin amqpAdmin;
    private RabbitMQConfig config;

    public JobQueueConfigurer(ConnectionFactory factory, JobMessageListener jobMessageListener, RabbitMQConfig config, AmqpAdmin amqpAdmin) {
        this.connectionFactory = factory;
        this.jobMessageLister = jobMessageListener;
        this.config = config;
        this.amqpAdmin = amqpAdmin;
    }

    @Bean
    public SimpleMessageListenerContainer jobMessageContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setMessageConverter(new Jackson2JsonMessageConverter());
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
        DirectExchange directExchange = new DirectExchange(config.getExchange(), true, true);
        amqpAdmin.declareExchange(directExchange);
        return directExchange;
    }

    @Bean
    public Binding jobBinding() {
        Binding b =BindingBuilder.bind(jobQueue()).to(jobDirectExchange()).with(config.getRoutingKey());
        this.amqpAdmin.declareBinding(b);
        return b;
    }

}