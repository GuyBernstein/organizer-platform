package com.organizer.platform.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

public class AccessControlResponse {
    private final boolean isAllowed;
    private final HttpStatus status;
    private final String message;
    private final String details;

    private AccessControlResponse(boolean isAllowed, HttpStatus status, String message, String details) {
        this.isAllowed = isAllowed;
        this.status = status;
        this.message = message;
        this.details = details;
    }

    public static AccessControlResponse allowed() {
        return new AccessControlResponse(true, HttpStatus.OK, "Access granted", null);
    }

    public static AccessControlResponse denied(HttpStatus status, String message, String details) {
        return new AccessControlResponse(false, status, message, details);
    }

    public boolean isAllowed() {
        return isAllowed;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getDetails() {
        return details;
    }

    public ResponseEntity<?> toResponseEntity() {
        Map<String, Object> body = new HashMap<>();
        body.put("message", message);
        if (details != null) {
            body.put("details", details);
        }
        return ResponseEntity.status(status).body(body);
    }
}
