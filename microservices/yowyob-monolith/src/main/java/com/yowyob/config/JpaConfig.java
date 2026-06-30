package com.yowyob.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration JPA multi-datasources pour le monolithe.
 * Gère trois datasources séparés : auth, users et listings,
 * chacun avec son propre EntityManagerFactory et TransactionManager.
 *
 * Mise à jour Spring Boot 4 : DataSourceProperties et EntityManagerFactoryBuilder
 * ont été déplacés dans des modules séparés. Cette configuration utilise
 * HikariDataSource directement et configure Hibernate explicitement.
 *
 * @author YowYob Team
 * @since 2.0.0
 */
@Configuration
public class JpaConfig {

    // ==================== Auth Datasource Properties ====================

    @Value("${spring.datasource.auth.url:${spring.datasource.url}}")
    private String authUrl;
    @Value("${spring.datasource.auth.username:${spring.datasource.username}}")
    private String authUsername;
    @Value("${spring.datasource.auth.password:${spring.datasource.password}}")
    private String authPassword;

    // ==================== Users Datasource Properties ====================

    @Value("${spring.datasource.users.url:${SPRING_DATASOURCE_USERS_URL:jdbc:postgresql://localhost:5432/yowyob_users}}")
    private String usersUrl;
    @Value("${spring.datasource.users.username:postgres}")
    private String usersUsername;
    @Value("${spring.datasource.users.password:postgres}")
    private String usersPassword;

    // ==================== Listings Datasource Properties ====================

    @Value("${spring.datasource.listings.url:${SPRING_DATASOURCE_LISTINGS_URL:jdbc:postgresql://localhost:5432/yowyob_listings}}")
    private String listingsUrl;
    @Value("${spring.datasource.listings.username:postgres}")
    private String listingsUsername;
    @Value("${spring.datasource.listings.password:postgres}")
    private String listingsPassword;

    private static Map<String, Object> jpaProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.show_sql", false);
        props.put("hibernate.format_sql", true);
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        props.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        return props;
    }

    private HikariDataSource buildDataSource(String url, String username, String password) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setDriverClassName("org.postgresql.Driver");
        return ds;
    }

    private LocalContainerEntityManagerFactoryBean buildEmf(DataSource dataSource, String packagesToScan,
            String persistenceUnit) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan(packagesToScan);
        emf.setPersistenceUnitName(persistenceUnit);
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setJpaPropertyMap(jpaProperties());
        return emf;
    }

    // ==================== Auth Datasource (Primary) ====================

    @Primary
    @Bean(name = "authDataSource")
    public DataSource authDataSource() {
        return buildDataSource(authUrl, authUsername, authPassword);
    }

    @Primary
    @Bean(name = "authEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean authEntityManagerFactory() {
        return buildEmf(authDataSource(), "com.yowyob.auth.entity", "auth");
    }

    @Primary
    @Bean(name = "authTransactionManager")
    public PlatformTransactionManager authTransactionManager(
            @Qualifier("authEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    // ==================== Users Datasource ====================

    @Bean(name = "usersDataSource")
    public DataSource usersDataSource() {
        return buildDataSource(usersUrl, usersUsername, usersPassword);
    }

    @Bean(name = "usersEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean usersEntityManagerFactory() {
        return buildEmf(usersDataSource(), "com.yowyob.user.entity", "users");
    }

    @Bean(name = "usersTransactionManager")
    public PlatformTransactionManager usersTransactionManager(
            @Qualifier("usersEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    // ==================== Listings Datasource ====================

    @Bean(name = "listingsDataSource")
    public DataSource listingsDataSource() {
        return buildDataSource(listingsUrl, listingsUsername, listingsPassword);
    }

    @Bean(name = "listingsEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean listingsEntityManagerFactory() {
        return buildEmf(listingsDataSource(), "com.yowyob.listing.entity", "listings");
    }

    @Bean(name = "listingsTransactionManager")
    public PlatformTransactionManager listingsTransactionManager(
            @Qualifier("listingsEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
