package com.yowyob.auth.domain.model;

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
public class User {

    private UUID id;
    private String name;
    private String email;
    private String password;
    private String phone;
    private String avatarUrl;
    private Role role;
    private Boolean emailVerified;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Role {
        USER, ADMIN, MERCHANT
    }

    public enum Status {
        ACTIVE, INACTIVE, BANNED
    }
}
