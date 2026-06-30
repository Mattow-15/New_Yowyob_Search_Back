/**
 * Standard error response DTO for API error handling.
 * Contains error details, error code, and timestamp.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2024-01-14
 * @updated 2025-02-11
 */
package com.yowyob.auth.dto;

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
