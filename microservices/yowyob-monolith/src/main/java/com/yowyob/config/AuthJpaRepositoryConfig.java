package com.yowyob.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration JPA pour les repositories du module authentification.
 * Connecte les repositories au datasource auth avec son EntityManager dédié.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.yowyob.auth.repository", entityManagerFactoryRef = "authEntityManagerFactory", transactionManagerRef = "authTransactionManager")
public class AuthJpaRepositoryConfig {
}
