package com.yowyob.listing.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String CRAWLER_LISTINGS_TOPIC = "crawler.listings.events";
    public static final String CRAWLER_LISTINGS_DLT   = "crawler.listings.events.DLT";

    @Bean
    public NewTopic crawlerListingsTopic() {
        return TopicBuilder.name(CRAWLER_LISTINGS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic crawlerListingsDlt() {
        return TopicBuilder.name(CRAWLER_LISTINGS_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
