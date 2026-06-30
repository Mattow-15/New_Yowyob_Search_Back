package com.yowyob.geo.infrastructure.config;

import com.yowyob.geo.infrastructure.adapters.in.web.dto.GeoLocationDto;
import com.yowyob.geo.infrastructure.adapters.in.web.dto.GeocodeResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, GeocodeResponse> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<GeocodeResponse> serializer = new Jackson2JsonRedisSerializer<>(
                GeocodeResponse.class);
        RedisSerializationContext.RedisSerializationContextBuilder<String, GeocodeResponse> builder = RedisSerializationContext
                .newSerializationContext(new StringRedisSerializer());
        RedisSerializationContext<String, GeocodeResponse> context = builder.value(serializer).build();
        return new ReactiveRedisTemplate<>(factory, context);
    }

    @Bean
    public ReactiveRedisTemplate<String, GeoLocationDto> reactiveRedisTemplateForGeoLocation(
            ReactiveRedisConnectionFactory factory) {
        Jackson2JsonRedisSerializer<GeoLocationDto> serializer = new Jackson2JsonRedisSerializer<>(
                GeoLocationDto.class);
        RedisSerializationContext.RedisSerializationContextBuilder<String, GeoLocationDto> builder = RedisSerializationContext
                .newSerializationContext(new StringRedisSerializer());
        RedisSerializationContext<String, GeoLocationDto> context = builder.value(serializer).build();
        return new ReactiveRedisTemplate<>(factory, context);
    }
}
