package com.clearfund.exception;

/**
 * Thrown when an uploaded (simplified) SWIFT-style message is missing a
 * required field or contains a value we cannot interpret. Surfaced to the
 * client as HTTP 422.
 */
public class SwiftParseException extends RuntimeException {

    public SwiftParseException(String message) {
        super(message);
    }
}
