package com.themis.engine.api.common.error;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Structured error response returned by the API when operations fail.
 */
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    List<String> details
) {}
