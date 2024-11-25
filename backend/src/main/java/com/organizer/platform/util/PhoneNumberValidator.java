package com.organizer.platform.util;

import lombok.Getter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Map;

public class PhoneNumberValidator {

    public static class ValidationResult {
        private final boolean isValid;
        @Getter
        private final String internationalFormat;
        @Getter
        private final ResponseEntity<?> errorResponse;

        private ValidationResult(boolean isValid, String internationalFormat, ResponseEntity<?> errorResponse) {
            this.isValid = isValid;
            this.internationalFormat = internationalFormat;
            this.errorResponse = errorResponse;
        }

        public static ValidationResult success(String internationalFormat) {
            return new ValidationResult(true, internationalFormat, null);
        }

        public static ValidationResult error(ResponseEntity<?> errorResponse) {
            return new ValidationResult(false, null, errorResponse);
        }

        public boolean isValid() {
            return isValid;
        }

    }

    /**
     * Validates phone number format and performs access control check
     * @param phoneNumber The phone number to validate
     * @param authentication The authentication object
     * @param accessControlChecker The function to check access control
     * @return ValidationResult containing the result of validation and access control check
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
     * Validates phone number format and converts to international format
     * @param phoneNumber The phone number to validate
     * @return ValidationResult containing the result
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

    @FunctionalInterface
    public interface AccessControlChecker {
        AccessControlResponse check(Authentication authentication, String phoneNumber);
    }
}