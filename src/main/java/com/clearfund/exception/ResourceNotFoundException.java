package com.clearfund.exception;

/** Thrown when a referenced entity (fund, account, order) does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entity, String key) {
        return new ResourceNotFoundException("%s not found: %s".formatted(entity, key));
    }
}
