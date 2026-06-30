package com.yowyob.auth.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank(message = "Google Token is required")
    private String token;
}
