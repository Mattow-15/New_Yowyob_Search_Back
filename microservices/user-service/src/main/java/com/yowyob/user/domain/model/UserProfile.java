package com.yowyob.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private UUID id;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
