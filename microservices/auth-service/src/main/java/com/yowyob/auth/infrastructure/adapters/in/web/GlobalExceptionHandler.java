package com.yowyob.auth.infrastructure.adapters.in.web;

import com.yowyob.auth.infrastructure.adapters.in.web.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: ", ex);
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .error("RUNTIME_ERROR")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.error("Validation error: {}", message);

        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .message(message)
                .error("VALIDATION_ERROR")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unhandled exception: ", ex);
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .message("Internal server error: " + ex.getMessage())
                .error("SERVER_ERROR")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
