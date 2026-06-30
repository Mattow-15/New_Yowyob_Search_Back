package com.yowyob.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration JPA pour les repositories du module utilisateurs.
 * Connecte les repositories au datasource users avec son EntityManager dédié.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.yowyob.user.repository", entityManagerFactoryRef = "usersEntityManagerFactory", transactionManagerRef = "usersTransactionManager")
public class UsersJpaRepositoryConfig {
}
