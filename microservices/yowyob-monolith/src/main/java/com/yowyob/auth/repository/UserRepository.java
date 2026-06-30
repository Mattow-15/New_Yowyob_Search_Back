package com.yowyob.auth.repository;

import com.yowyob.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA pour l'entité {@link User}.
 * Fournit les opérations CRUD et les requêtes personnalisées pour les
 * utilisateurs.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);
}
