/**
 * Exception thrown when Google OAuth authentication fails.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2025-02-11
 */
package com.yowyob.auth.exception;

public class GoogleLoginException extends RuntimeException {

    public GoogleLoginException(String message) {
        super("Google Login failed: " + message);
    }

    public GoogleLoginException(String message, Throwable cause) {
        super("Google Login failed: " + message, cause);
    }
}
