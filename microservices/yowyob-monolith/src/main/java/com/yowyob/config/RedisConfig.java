package com.yowyob.config;

import com.yowyob.geo.dto.GeoLocationDto;
import com.yowyob.geo.dto.GeocodeResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration Redis réactive pour le cache applicatif.
 * Fournit des templates typés pour String, GeocodeResponse et GeoLocationDto.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Configuration
public class RedisConfig {

        @Primary
        @Bean
        public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
                        ReactiveRedisConnectionFactory connectionFactory) {
                RedisSerializationContext<String, String> context = RedisSerializationContext
                                .<String, String>newSerializationContext(new StringRedisSerializer())
                                .build();
                return new ReactiveRedisTemplate<>(connectionFactory, context);
        }

        @Bean
        public ReactiveRedisTemplate<String, GeocodeResponse> reactiveRedisTemplateForGeocode(
                        ReactiveRedisConnectionFactory connectionFactory) {
                Jackson2JsonRedisSerializer<GeocodeResponse> valueSerializer = new Jackson2JsonRedisSerializer<>(
                                GeocodeResponse.class);
                RedisSerializationContext<String, GeocodeResponse> context = RedisSerializationContext
                                .<String, GeocodeResponse>newSerializationContext(new StringRedisSerializer())
                                .value(valueSerializer)
                                .build();
                return new ReactiveRedisTemplate<>(connectionFactory, context);
        }

        @Bean("reactiveRedisTemplateForGeoLocation")
        public ReactiveRedisTemplate<String, GeoLocationDto> reactiveRedisTemplateForGeoLocation(
                        ReactiveRedisConnectionFactory connectionFactory) {
                Jackson2JsonRedisSerializer<GeoLocationDto> valueSerializer = new Jackson2JsonRedisSerializer<>(
                                GeoLocationDto.class);
                RedisSerializationContext<String, GeoLocationDto> context = RedisSerializationContext
                                .<String, GeoLocationDto>newSerializationContext(new StringRedisSerializer())
                                .value(valueSerializer)
                                .build();
                return new ReactiveRedisTemplate<>(connectionFactory, context);
        }
}
