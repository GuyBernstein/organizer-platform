package com.organizer.platform.util;

import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;

/**
 * Utility class for validating phone numbers and performing access control checks.
 * Supports Israeli phone number formats (starting with '05' or '972') and converts
 * them to international format.
 */
public class PhoneNumberValidator {

    /**
     * Inner class representing the result of phone number validation.
     * Encapsulates the validation status, converted international format,
     * and any error response if validation fails.
     */
    public static class ValidationResult {
        private final boolean isValid;
        @Getter
        private final String internationalFormat;  // The phone number in international format (if valid)
        @Getter
        private final ResponseEntity<?> errorResponse;  // Error response details (if invalid)

        /**
         * Private constructor to enforce usage of factory methods.
         *
         * @param isValid Whether the phone number is valid
         * @param internationalFormat The converted international format (null if invalid)
         * @param errorResponse Error response details (null if valid)
         */
        private ValidationResult(boolean isValid, String internationalFormat, ResponseEntity<?> errorResponse) {
            this.isValid = isValid;
            this.internationalFormat = internationalFormat;
            this.errorResponse = errorResponse;
        }

        /**
         * Factory method for creating a successful validation result.
         *
         * @param internationalFormat The phone number in international format
         * @return A ValidationResult instance indicating success
         */
        public static ValidationResult success(String internationalFormat) {
            return new ValidationResult(true, internationalFormat, null);
        }

        /**
         * Factory method for creating a failed validation result.
         *
         * @param errorResponse The error response containing validation failure details
         * @return A ValidationResult instance indicating failure
         */
        public static ValidationResult error(ResponseEntity<?> errorResponse) {
            return new ValidationResult(false, null, errorResponse);
        }

        /**
         * @return true if the phone number is valid, false otherwise
         */
        public boolean isValid() {
            return isValid;
        }
    }

    /**
     * Validates a phone number format and performs access control check in a single operation.
     * This method combines both validation and access control to ensure the phone number
     * is both valid and the authenticated user has permission to use it.
     *
     * Example usage:
     * {@code
     * ResponseEntity<?> response = validateAndCheckAccess(
     *     phoneNumber,
     *     authentication,
     *     this::checkAccessControl
     * );
     * if (response != null) {
     *     return response; // Validation or access control failed
     * }
     * // Continue with business logic
     * }
     *
     * @param phoneNumber The phone number to validate (formats: 05XXXXXXXX or 972XXXXXXXXX)
     * @param authentication The Spring Security authentication object
     * @param accessControlChecker Function to perform access control check
     * @return null if validation and access check pass, otherwise returns error ResponseEntity
     */
    public static ResponseEntity<?> validateAndCheckAccess(
            String phoneNumber,
            Authentication authentication,
            AccessControlChecker accessControlChecker) {

        ValidationResult validationResult = validatePhoneNumber(phoneNumber);
        if (!validationResult.isValid()) {
            return validationResult.getErrorResponse();
        }

        // Check access control
        AccessControlResponse accessControl = accessControlChecker.check(authentication, validationResult.getInternationalFormat());
        if (!accessControl.isAllowed()) {
            return accessControl.toResponseEntity();
        }

        return null; // Indicates successful validation and access check
    }

    /**
     * Validates a phone number format and converts it to international format if valid.
     * Supports two formats:
     * 1. Local format starting with '05' (e.g., 0509603888)
     * 2. International format starting with '972' (e.g., 972509603888)
     *
     * @param phoneNumber The phone number to validate
     * @return ValidationResult containing either:
     *         - Success: international format phone number (972XXXXXXXXX)
     *         - Error: ResponseEntity with error details
     */
    public static ValidationResult validatePhoneNumber(String phoneNumber) {
        // Check if phone starts with 0 (0509603888)
        if (phoneNumber.matches("^05\\d{8}$")) {
            return ValidationResult.success("972" + phoneNumber.substring(1));
        }
        // Check if phone starts with 972 (972509603888)
        else if (phoneNumber.matches("^972\\d{9}$")) {
            return ValidationResult.success(phoneNumber);
        }
        // Invalid format
        else {
            ResponseEntity<?> errorResponse = ResponseEntity.badRequest()
                    .body(Map.of(
                            "message", "Invalid phone number format",
                            "details", "Phone number must start with '05' or '972'"
                    ));
            return ValidationResult.error(errorResponse);
        }
    }

    /**
     * Functional interface for performing access control checks on phone numbers.
     * Implementations should verify if the authenticated user has permission
     * to perform operations with the given phone number.
     */
    @FunctionalInterface
    public interface AccessControlChecker {
        /**
         * Check if the authenticated user has access to operate with the given phone number.
         *
         * @param authentication The Spring Security authentication object
         * @param phoneNumber The phone number in international format
         * @return AccessControlResponse indicating whether access is allowed
         */
        AccessControlResponse check(Authentication authentication, String phoneNumber);
    }
}