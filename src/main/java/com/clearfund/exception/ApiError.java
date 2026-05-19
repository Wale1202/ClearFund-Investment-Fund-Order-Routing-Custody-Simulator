package com.clearfund.exception;

import java.time.Instant;
import java.util.List;

/**
 * Consistent error payload returned for every handled exception.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        List<String> messages,
        String path
) {
    public static ApiError of(int status, String error, List<String> messages, String path) {
        return new ApiError(Instant.now(), status, error, messages, path);
    }
}
