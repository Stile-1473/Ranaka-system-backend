package Ranaka.ranaka.common.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ApiErrorResponse {
    // Stable machine-readable code the frontend can branch on if needed.
    private String code;

    // Human-facing summary we can show in toast messages or form errors.
    private String message;

    // Extra context such as field validation problems or business rule details.
    private Object details;

    // Timestamp helps support teams line up API errors with server logs.
    private LocalDateTime timestamp;

    // The API path that produced the error, for example /api/v1/requests/12/submit.
    private String path;

    public static ApiErrorResponse of(String code, String message, Object details, String path) {
        return ApiErrorResponse.builder()
                .code(code)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    public static ApiErrorResponse of(String code, String message, String path) {
        // Use an empty details object when there is nothing more specific to return.
        return of(code, message, Map.of(), path);
    }
}
