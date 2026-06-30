package com.yowyob.auth.application.ports.out;

import com.yowyob.auth.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
    Boolean existsByEmail(String email);
    User save(User user);
}
