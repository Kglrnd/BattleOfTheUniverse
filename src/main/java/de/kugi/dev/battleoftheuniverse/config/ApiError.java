package de.kugi.dev.battleoftheuniverse.config;

import java.time.Instant;
import java.util.Map;

public record ApiError(Instant timestamp, int status, String message, Map<String, String> fieldErrors) {
    public ApiError(int status, String message) {
        this(Instant.now(), status, message, Map.of());
    }

    public ApiError(int status, String message, Map<String, String> fieldErrors) {
        this(Instant.now(), status, message, fieldErrors);
    }
}
