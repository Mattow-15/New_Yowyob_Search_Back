package com.yowyob.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO pour la requête de connexion via Google OAuth.
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Data
public class GoogleLoginRequest {
    @NotBlank(message = "Google Token is required")
    private String token;
}
