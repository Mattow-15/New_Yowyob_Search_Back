/**
 * Exception thrown when a user account is not in an active state.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2025-02-11
 */
package com.yowyob.auth.exception;

public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException() {
        super("Account is not active");
    }

    public AccountNotActiveException(String email) {
        super("Account is not active for user: " + email);
    }
}
