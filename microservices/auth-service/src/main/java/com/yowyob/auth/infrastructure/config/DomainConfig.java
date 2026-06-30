package com.yowyob.auth.infrastructure.config;

import com.yowyob.auth.application.ports.out.*;
import com.yowyob.auth.application.services.AuthApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public AuthApplicationService authApplicationService(
            UserRepositoryPort userRepositoryPort,
            PasswordEncoderPort passwordEncoderPort,
            TokenServicePort tokenServicePort,
            IdentityProviderPort identityProviderPort,
            UserEventPublisherPort userEventPublisherPort,
            GeoLocatorPort geoLocatorPort) {
        return new AuthApplicationService(
                userRepositoryPort,
                passwordEncoderPort,
                tokenServicePort,
                identityProviderPort,
                userEventPublisherPort,
                geoLocatorPort
        );
    }
}
