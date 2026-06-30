package com.yowyob.auth.infrastructure.config;

import com.yowyob.auth.application.ports.out.PasswordEncoderPort;
import com.yowyob.auth.application.ports.out.UserRepositoryPort;
import com.yowyob.auth.domain.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Initialize system users on application startup
 */
@Configuration
@Slf4j
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeDefaultUsers(UserRepositoryPort userRepositoryPort, PasswordEncoderPort passwordEncoderPort) {
        return args -> {
            // --- Crawler system user ---
            createUserIfNotExists(userRepositoryPort, passwordEncoderPort,
                    "Yowyob Crawler System", "crawler@yowyob.system",
                    "crawler_secure_password_123", User.Role.USER, true);

            // --- Admin test user ---
            createUserIfNotExists(userRepositoryPort, passwordEncoderPort,
                    "Admin Yowyob", "admin@yowyob.com",
                    "admin123", User.Role.USER, false);

            // --- Standard test user ---
            createUserIfNotExists(userRepositoryPort, passwordEncoderPort,
                    "User Test", "user@yowyob.com",
                    "user123", User.Role.USER, false);
        };
    }

    private void createUserIfNotExists(UserRepositoryPort repo, PasswordEncoderPort encoder,
                                        String name, String email, String password,
                                        User.Role role, boolean emailVerified) {
        if (repo.existsByEmail(email)) {
            log.info("User already exists: {}", email);
            return;
        }
        User user = User.builder()
                .name(name)
                .email(email)
                .password(encoder.encode(password))
                .role(role)
                .status(User.Status.ACTIVE)
                .emailVerified(emailVerified)
                .build();
        repo.save(user);
        log.info("✓ User created: {} ({})", email, role);
    }
}
