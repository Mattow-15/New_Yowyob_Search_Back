package com.yowyob.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "notification.email.queue";
    public static final String EXCHANGE_NAME = "listing.events";

    @Bean
    public TopicExchange listingExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue emailQueue() {
        return new Queue(QUEUE_NAME);
    }

    @Bean
    public Binding binding(Queue emailQueue, TopicExchange listingExchange) {
        return BindingBuilder.bind(emailQueue).to(listingExchange).with("listing.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
