package com.yowyob.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration RabbitMQ pour la messagerie événementielle.
 * Définit l'exchange fanout et les queues pour la synchronisation
 * des listings entre les modules search et notification.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Configuration
public class RabbitMQConfig {

    public static final String LISTING_EXCHANGE = "listing.exchange";
    public static final String SEARCH_QUEUE = "search.listing.queue";
    public static final String NOTIFICATION_QUEUE = "notification.listing.queue";

    @Bean
    public FanoutExchange listingExchange() {
        return new FanoutExchange(LISTING_EXCHANGE);
    }

    @Bean
    public Queue searchQueue() {
        return new Queue(SEARCH_QUEUE, true);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding searchBinding(Queue searchQueue, FanoutExchange listingExchange) {
        return BindingBuilder.bind(searchQueue).to(listingExchange);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, FanoutExchange listingExchange) {
        return BindingBuilder.bind(notificationQueue).to(listingExchange);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
