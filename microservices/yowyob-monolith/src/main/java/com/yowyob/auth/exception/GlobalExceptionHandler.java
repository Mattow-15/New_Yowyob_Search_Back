/**
 * Global exception handler for the application.
 * Catches and handles specific business exceptions and generic errors.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2024-01-14
 * @updated 2025-02-11
 */
package com.yowyob.auth.exception;

import com.yowyob.auth.dto.ErrorResponse;
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

    /**
     * Handles email already exists exception during registration.
     *
     * @param ex the exception thrown when email is duplicate
     * @return error response with HTTP 409 Conflict
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        log.warn("Email conflict: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .error("EMAIL_ALREADY_EXISTS")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles invalid credentials exception during login.
     *
     * @param ex the exception thrown when credentials are invalid
     * @return error response with HTTP 401 Unauthorized
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .error("INVALID_CREDENTIALS")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles account not active exception.
     *
     * @param ex the exception thrown when a user account is inactive
     * @return error response with HTTP 403 Forbidden
     */
    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotActive(AccountNotActiveException ex) {
        log.warn("Inactive account access attempt: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .error("ACCOUNT_NOT_ACTIVE")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles Google login exception.
     *
     * @param ex the exception thrown when Google authentication fails
     * @return error response with HTTP 401 Unauthorized
     */
    @ExceptionHandler(GoogleLoginException.class)
    public ResponseEntity<ErrorResponse> handleGoogleLoginException(GoogleLoginException ex) {
        log.error("Google login error: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .error("GOOGLE_LOGIN_FAILED")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handles resource not found exception.
     *
     * @param ex the exception thrown when a resource cannot be found
     * @return error response with HTTP 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .error("RESOURCE_NOT_FOUND")
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles validation errors from request body validation.
     *
     * @param ex the validation exception
     * @return error response with HTTP 400 Bad Request
     */
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

    /**
     * Handles generic runtime exceptions not caught by specific handlers.
     *
     * @param ex the runtime exception
     * @return error response with HTTP 400 Bad Request
     */
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

    /**
     * Handles unhandled exceptions as a last resort.
     *
     * @param ex the exception
     * @return error response with HTTP 500 Internal Server Error
     */
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
