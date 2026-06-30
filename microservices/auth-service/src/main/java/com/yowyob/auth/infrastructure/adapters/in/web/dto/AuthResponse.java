package com.yowyob.auth.infrastructure.adapters.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    private Boolean success;
    private String message;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private UserDto user;
    /** Position de l'utilisateur déduite de son IP à la connexion (null si indéterminée). */
    private LocationDto location;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private String id;
        private String name;
        private String email;
        private String role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDto {
        private String ip;
        private String city;
        private String country;
        private double latitude;
        private double longitude;
    }
}
