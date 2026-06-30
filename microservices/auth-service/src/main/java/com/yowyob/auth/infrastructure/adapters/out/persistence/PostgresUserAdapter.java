package com.yowyob.auth.infrastructure.adapters.out.persistence;

import com.yowyob.auth.application.ports.out.UserRepositoryPort;
import com.yowyob.auth.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostgresUserAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity saved = userJpaRepository.save(entity);
        return toDomain(saved);
    }

    // --- Mappings ---

    private User toDomain(UserJpaEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .phone(entity.getPhone())
                .avatarUrl(entity.getAvatarUrl())
                .role(User.Role.valueOf(entity.getRole().name()))
                .emailVerified(entity.getEmailVerified())
                .status(User.Status.valueOf(entity.getStatus().name()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UserJpaEntity toEntity(User domain) {
        if (domain == null) return null;
        return UserJpaEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .email(domain.getEmail())
                .password(domain.getPassword())
                .phone(domain.getPhone())
                .avatarUrl(domain.getAvatarUrl())
                .role(UserJpaEntity.Role.valueOf(domain.getRole().name()))
                .emailVerified(domain.getEmailVerified())
                .status(UserJpaEntity.Status.valueOf(domain.getStatus().name()))
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
