package com.medical.inventory.exception;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    boolean success,
    int status,
    String error,
    String message,
    List<String> details,
    String path,
    Instant timestamp
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(false, status, error, message, List.of(), path, Instant.now());
    }

    public static ErrorResponse of(int status, String error, String message, List<String> details, String path) {
        return new ErrorResponse(false, status, error, message, details != null ? List.copyOf(details) : List.of(), path, Instant.now());
    }
}
