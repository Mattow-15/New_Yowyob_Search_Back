/**
 * Exception thrown when authentication fails due to invalid credentials.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2025-02-11
 */
package com.yowyob.auth.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
