package com.yowyob.auth.infrastructure.adapters.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class ErrorResponse {
    private Boolean success;
    private String message;
    private String error;
    private LocalDateTime timestamp;
}
