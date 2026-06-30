package com.yowyob.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration JPA pour les repositories du module listings.
 * Connecte les repositories au datasource listings avec son EntityManager
 * dédié.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.yowyob.listing.repository", entityManagerFactoryRef = "listingsEntityManagerFactory", transactionManagerRef = "listingsTransactionManager")
public class ListingsJpaRepositoryConfig {
}
