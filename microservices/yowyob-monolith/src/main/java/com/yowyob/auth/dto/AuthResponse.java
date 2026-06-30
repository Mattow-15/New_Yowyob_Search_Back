/**
 * Authentication response DTO containing JWT tokens and user information.
 * Returned after successful registration or login operations.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2024-01-14
 * @updated 2025-02-11
 */
package com.yowyob.auth.dto;

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
}
