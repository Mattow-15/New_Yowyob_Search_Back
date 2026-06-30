/**
 * Exception thrown when a requested resource cannot be found.
 * @author Matteo Owona, Rouchda Yampen
 * @date 2025-02-11
 */
package com.yowyob.auth.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource_type, String resource_id) {
        super(resource_type + " not found with id: " + resource_id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
