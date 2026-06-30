package com.yowyob.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO pour la requête de connexion manuelle (email/mot de passe).
 *
 * @author YowYob Team
 * @since 1.0.0
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
