package com.yowyob.user.infrastructure.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    private String email;
    private String firstName;
    private String lastName;
    private String bio;
    private String phoneNumber;
    private String address;
    private String city;
    private String country;
    private String avatarUrl;
    private String socialLinksJson;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
