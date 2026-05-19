package com.clearfund.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Translates exceptions into a consistent {@link ApiError} response so the
 * controllers never deal with error formatting.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        List<String> messages = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage())
                .sorted()
                .toList();
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", messages, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex,
                                                   HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", List.of(ex.getMessage()), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex,
                                                       HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "BUSINESS_RULE_VIOLATION", List.of(ex.getMessage()), request);
    }

    @ExceptionHandler(SwiftParseException.class)
    public ResponseEntity<ApiError> handleSwiftParse(SwiftParseException ex,
                                                     HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "SWIFT_PARSE_ERROR",
                List.of(ex.getMessage()), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex,
                                                     HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                List.of("An unexpected error occurred"), request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error,
                                           List<String> messages, HttpServletRequest request) {
        ApiError body = ApiError.of(status.value(), error, messages, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
