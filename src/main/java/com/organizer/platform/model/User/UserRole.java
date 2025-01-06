package com.organizer.platform.model.User;

/**
 * UserRole defines the permission levels available in the platform.
 * This enum is used for role-based access control throughout the application.
 */
public enum UserRole {
    // Full system access with administrative privileges
    ADMIN,

    // Standard authenticated user with normal access rights
    USER,

    // Default state for new accounts pending authorization
    UNAUTHORIZED
}