package com.organizer.platform.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility class that handles authorization responses in a standardized format.
 * This class is used to determine whether a user has the required permissions
 * and generates appropriate HTTP responses.
 */
public class AccessControlResponse {
    /** Indicates whether the access request is allowed */
    private final boolean isAllowed;

    /** The HTTP status code to be returned in the response */
    private final HttpStatus status;

    /** A human-readable message describing the authorization result */
    private final String message;

    /** Additional details about the authorization decision (optional) */
    private final String details;

    /**
     * Private constructor to enforce usage of factory methods.
     *
     * @param isAllowed Whether the access is permitted
     * @param status The HTTP status code
     * @param message The response message
     * @param details Additional details (can be null)
     */
    private AccessControlResponse(boolean isAllowed, HttpStatus status, String message, String details) {
        this.isAllowed = isAllowed;
        this.status = status;
        this.message = message;
        this.details = details;
    }

    /**
     * Factory method for creating a successful authorization response.
     *
     * @return An AccessControlResponse instance with OK status and access granted
     */
    public static AccessControlResponse allowed() {
        return new AccessControlResponse(true, HttpStatus.OK, "Access granted", null);
    }

    /**
     * Factory method for creating a failed authorization response.
     *
     * @param status The HTTP status code (e.g., FORBIDDEN, UNAUTHORIZED)
     * @param message The error message
     * @param details Additional error details
     * @return An AccessControlResponse instance with access denied
     */
    public static AccessControlResponse denied(HttpStatus status, String message, String details) {
        return new AccessControlResponse(false, status, message, details);
    }

    /**
     * @return true if access is allowed, false otherwise
     */
    public boolean isAllowed() {
        return isAllowed;
    }

    /**
     * @return the HTTP status code
     */
    public HttpStatus getStatus() {
        return status;
    }

    /**
     * @return the response message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return additional details if present, null otherwise
     */
    public String getDetails() {
        return details;
    }

    /**
     * Converts this AccessControlResponse into a Spring ResponseEntity.
     * Creates a standardized response format with message and optional details.
     *
     * @return A ResponseEntity containing the authorization response data
     */
    public ResponseEntity<?> toResponseEntity() {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        if (details != null) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}