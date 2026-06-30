package com.yowyob.user.infrastructure.config;

import com.yowyob.user.application.ports.out.SearchHistoryRepositoryPort;
import com.yowyob.user.application.ports.out.UserRepositoryPort;
import com.yowyob.user.application.services.UserApplicationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public UserApplicationService userApplicationService(
            UserRepositoryPort userRepositoryPort,
            SearchHistoryRepositoryPort searchHistoryRepositoryPort) {
        return new UserApplicationService(userRepositoryPort, searchHistoryRepositoryPort);
    }
}
