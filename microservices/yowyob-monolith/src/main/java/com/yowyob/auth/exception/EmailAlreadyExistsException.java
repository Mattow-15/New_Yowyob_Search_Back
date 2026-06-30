/**
 * Exception thrown when a user tries to register with an email that is already in use.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2025-02-11
 */
package com.yowyob.auth.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email already exists: " + email);
    }
}
