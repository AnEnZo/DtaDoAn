package com.example.DtaAssigement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Standard error contract returned to clients.
 * Serialized by Jackson (which escapes quotes/newlines/backslashes correctly),
 * so error messages from any source are always valid JSON.
 *
 * Diagnostic fields (exception/method/stackTrace) are only populated when
 * detailed error exposure is enabled (dev profile) and are omitted from the
 * JSON when null.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    // Diagnostic-only fields (omitted unless detailed errors are enabled)
    private String exception;
    private String method;
    private String stackTrace;
}
