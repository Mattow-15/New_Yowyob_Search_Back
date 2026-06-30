package com.yowyob.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité JPA représentant le profil étendu d'un utilisateur.
 * Stocke les informations personnelles, contact et avatar.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Entity
@Table(name = "user_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId; // Linked to Auth Service User ID

    private String email;

    private String firstName;
    private String lastName;

    private String bio;

    // Optional override of contact info
    private String phoneNumber;

    private String address;
    private String city;
    private String country;

    private String avatarUrl;

    // Could store social links as JSON string or separate entity, keeping simple
    // for now
    private String socialLinksJson;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
