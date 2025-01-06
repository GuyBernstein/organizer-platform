package com.organizer.platform.controller;

import com.organizer.platform.model.User.AppUser;
import com.organizer.platform.model.User.UserRole;
import com.organizer.platform.service.User.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for managing user operations in the platform.
 * This controller provides endpoints for user management operations like authorization,
 * deauthorization, searching, and statistics.
 * <p>
 * These endpoints are documented and testable through Swagger UI at /swagger-ui.html
 * which allows for easy testing and validation of the API endpoints before
 * implementing the actual UI with Thymeleaf and Bootstrap.
 * <p>
 * Access to all endpoints is restricted to administrators only for security purposes.
 */
@RestController
@RequestMapping("/api/users")
public class UserManagementController {
    private final UserService userService;

    @Autowired
    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Validates whether the current user has administrative privileges.
     * This method is crucial for maintaining security across all endpoints
     * by ensuring that only administrators can perform user management operations.
     * It extracts the email from OAuth2 authentication to verify admin status.
     */
    private boolean isAdminUser(Authentication authentication) {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        return userService.isAdmin(email);
    }

    /**
     * Retrieves all users in the system regardless of their authorization status.
     * This endpoint is necessary for administrators to have a complete overview
     * of the user base, which is essential for effective user management and
     * monitoring system growth. The response includes both the total count and
     * the detailed user information for reporting purposes.
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
        if (!isAdminUser(authentication)) {
            return ResponseEntity.status(403)
                    .body("Only administrators can view all users");
        }

        List<AppUser> users = userService.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("total", users.size());
        response.put("users", users);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves only the authorized users who have been approved to use the system.
     * This endpoint helps administrators monitor active users and ensure
     * system access is properly controlled. It's particularly useful for
     * generating reports about active user engagement and system utilization.
     */
    @GetMapping("/authorized")
    public ResponseEntity<?> getAuthorizedUsers(Authentication authentication) {
        if (!isAdminUser(authentication)) {
            return ResponseEntity.status(403)
                    .body("Only administrators can view authorized users");
        }

        List<AppUser> authorizedUsers = userService.findByAuthorized(true);
        Map<String, Object> response = new HashMap<>();
        response.put("total", authorizedUsers.size());
        response.put("users", authorizedUsers);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all unauthorized users for admin review and approval.
     * This endpoint helps administrators manage the user onboarding process
     * by providing visibility into new or suspended users who need access
     * authorization. This is crucial for maintaining platform security
     * and ensuring proper access control.
     *
     * @param authentication Current user's authentication details
     * @return ResponseEntity containing list of unauthorized users or error message
     */
    @GetMapping("/unauthorized")
    public ResponseEntity<?> getUnauthorizedUsers(Authentication authentication) {
        if (!isAdminUser(authentication)) {
            return ResponseEntity.status(403)
                    .body("Only administrators can view unauthorized users");
        }

        List<AppUser> unauthorizedUsers = userService.findUnauthorizedUsers();
        Map<String, Object> response = new HashMap<>();
        response.put("total", unauthorizedUsers.size());
        response.put("users", unauthorizedUsers);

        return ResponseEntity.ok(response);
    }

    /**
     * Grants platform access to a user by setting their status to authorized
     * and assigning the standard USER role. This is a key part of the user
     * onboarding workflow, allowing admins to verify and approve new users
     * before they can access protected resources. This helps maintain platform
     * security and ensures proper user verification.
     *
     * @param userId ID of the user to authorize
     * @param authentication Current user's authentication details
     * @return ResponseEntity with updated user details or error message
     */
    @PostMapping("/{userId}/authorize")
    public ResponseEntity<?> authorizeUser(
            @PathVariable Long userId,
            Authentication authentication) {
        if (!isAdminUser(authentication)) {
            return ResponseEntity.status(403)
                    .body("Only administrators can authorize users");
        }

        return userService.findById(userId)
                .map(AppUser -> {
                    AppUser.setAuthorized(true);
                    AppUser.setRole(UserRole.USER);
                    AppUser updatedUser = userService.save(AppUser);
                    return ResponseEntity.ok(updatedUser);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Revokes a user's platform access by setting their status to unauthorized
     * and downgrading their role. This is essential for handling various
     * security scenarios such as:
     * - Suspending potentially compromised accounts
     * - Handling user offboarding
     * - Managing temporary access revocation
     * The user's account remains in the system but cannot access protected resources.
     *
     * @param userId ID of the user to deauthorize
     * @param authentication Current user's authentication details
     * @return ResponseEntity with updated user details or error message
     */
    @PostMapping("/{userId}/deauthorize")
    public ResponseEntity<?> deauthorizeUser(
            @PathVariable Long userId,
            Authentication authentication) {
        if (!isAdminUser(authentication)) {
            return ResponseEntity.status(403)
                    .body("Only administrators can deauthorize users");
        }

        return userService.findById(userId)
                .map(AppUser -> {
                    AppUser.setAuthorized(false);
                    AppUser.setRole(UserRole.UNAUTHORIZED);
                    AppUser updatedUser = userService.save(AppUser);
                    return ResponseEntity.ok(updatedUser);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Provides flexible user search functionality for administrators using
     * either email or WhatsApp number as identifiers. This endpoint supports:
     * - User support and troubleshooting
     * - Account verification
     * - User management tasks
     * The search is exclusive (either email OR WhatsApp) to ensure precise
     * user identification and prevent ambiguous results.
     *
     * @param email User's email address (optional)
     * @param whatsappNumber User's WhatsApp number (optional)
     * @param authentication Current user's authentication details
     * @return ResponseEntity with matching user details or appropriate error message
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String whatsappNumber,
            Authentication authentication) {
        if (!isAdminUser(authentication)) {
            return ResponseEntity.status(403)
                    .body("Only administrators can search users");
        }

        if (email != null) {
            return userService.findByEmail(email)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        if (whatsappNumber != null) {
            return userService.findByWhatsappNumber(whatsappNumber)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        return ResponseEntity.badRequest()
                .body("Either email or whatsappNumber parameter is required");
    }

    /**
     * Provides high-level statistics about user demographics in the platform
     * to help administrators make informed decisions about:
     * - User growth and adoption rates
     * - Authorization workflow effectiveness
     * - Administrative resource allocation
     *
     * This aggregated view helps identify trends and potential issues
     * in the user management process without having to manually count
     * or filter through individual user records.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats(Authentication authentication) {
        if (!isAdminUser(authentication)) {
            return ResponseEntity.status(403)
                    .body("Only administrators can view AppUser statistics");
        }

        List<AppUser> allUsers = userService.findAll();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", allUsers.size());
        stats.put("authorizedUsers", allUsers.stream()
                .filter(AppUser::isAuthorized).count());
        stats.put("unauthorizedUsers", allUsers.stream()
                .filter(AppUser -> !AppUser.isAuthorized()).count());
        stats.put("adminUsers", allUsers.stream()
                .filter(AppUser -> AppUser.getRole() == UserRole.ADMIN).count());

        return ResponseEntity.ok(stats);
    }

    /**
     * Links a WhatsApp number to a user while handling complex migration scenarios.
     * This method supports two key business cases:
     * 1. Normal linking of WhatsApp numbers to existing users
     * 2. Migration of temporary users' data when they finally create a full account
     * <p>
     * The temporary user migration is crucial for preserving user data when
     * users start interacting via WhatsApp before creating a full platform account.
     * This ensures no user data is lost during the transition from temporary
     * to permanent user status.
     * <p>
     * Security Note: Only administrators can perform this operation to prevent
     * unauthorized linking of WhatsApp numbers and potential impersonation.
     */
    @PostMapping("/{userId}/link-whatsapp")
    public ResponseEntity<?> linkWhatsAppNumber(
            @PathVariable Long userId,
            @RequestParam String whatsappNumber,
            Authentication authentication) {
        if (!isAdminUser(authentication)) {
            return ResponseEntity.status(403)
                    .body("Only administrators can link WhatsApp numbers");
        }

        // Check if WhatsApp number is already linked to another user
        Optional<AppUser> existingWhatsAppUser = userService.findByWhatsappNumber(whatsappNumber);
        if (existingWhatsAppUser.isPresent() && !existingWhatsAppUser.get().getId().equals(userId)) {
            // If the existing user is a temporary one (has temp email), migrate their data
            AppUser existing = existingWhatsAppUser.get();
            if (existing.getEmail().endsWith("@temp.platform.com")) {
                // Delete the temporary user
                userService.delete(existing);
            } else {
                return ResponseEntity.badRequest()
                        .body("WhatsApp number already linked to another user");
            }
        }

        return userService.findById(userId)
                .map(user -> {
                    user.setWhatsappNumber(whatsappNumber);
                    AppUser updatedUser = userService.save(user);
                    return ResponseEntity.ok(updatedUser);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Permanently removes a user from the system. This is a critical operation used for:
     * - Complying with data deletion requests (e.g., GDPR requirements)
     * - Removing spam or malicious accounts
     * - Cleaning up test or temporary accounts
     * <p>
     * This operation is restricted to administrators due to its irreversible nature
     * and the potential impact on system integrity and user data.
     * <p>
     * Note: Consider implementing soft delete if maintaining historical records
     * becomes a requirement in the future.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long userId,
            Authentication authentication) {
        if (!isAdminUser(authentication)) {
            return ResponseEntity.status(403)
                    .body("Only administrators can delete a user");
        }
        return userService.findById(userId)
                .map(user -> {
                    userService.delete(user);
                    return ResponseEntity.status(200)
                            .body("User with id " + userId + " has been deleted");
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}