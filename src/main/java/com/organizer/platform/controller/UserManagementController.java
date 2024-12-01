package com.organizer.platform.controller;

import com.organizer.platform.model.User.AppUser;
import com.organizer.platform.model.User.UserRole;
import com.organizer.platform.service.User.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {
    private static final Logger logger = LoggerFactory.getLogger(UserManagementController.class);

    private final UserService userService;

    @Autowired
    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    private boolean isAdminUser(Authentication authentication) {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        return userService.isAdmin(email);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
        if (!isAdminUser(authentication)) {
            logger.warn("Unauthorized access attempt to view all users by: {}",
                    authentication.getName());
            return ResponseEntity.status(403)
                    .body("Only administrators can view all users");
        }

        List<AppUser> users = userService.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("total", users.size());
        response.put("users", users);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/authorized")
    public ResponseEntity<?> getAuthorizedUsers(Authentication authentication) {
        if (!isAdminUser(authentication)) {
            logger.warn("Unauthorized access attempt to view authorized users by: {}",
                    authentication.getName());
            return ResponseEntity.status(403)
                    .body("Only administrators can view authorized users");
        }

        List<AppUser> authorizedUsers = userService.findByAuthorized(true);
        Map<String, Object> response = new HashMap<>();
        response.put("total", authorizedUsers.size());
        response.put("users", authorizedUsers);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/unauthorized")
    public ResponseEntity<?> getUnauthorizedUsers(Authentication authentication) {
        if (!isAdminUser(authentication)) {
            logger.warn("Unauthorized access attempt to view unauthorized users by: {}",
                    authentication.getName());
            return ResponseEntity.status(403)
                    .body("Only administrators can view unauthorized users");
        }

        List<AppUser> unauthorizedUsers = userService.findUnauthorizedUsers();
        Map<String, Object> response = new HashMap<>();
        response.put("total", unauthorizedUsers.size());
        response.put("users", unauthorizedUsers);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/authorize")
    public ResponseEntity<?> authorizeUser(
            @PathVariable Long userId,
            Authentication authentication) {
        if (!isAdminUser(authentication)) {
            logger.warn("Unauthorized access attempt to authorize AppUser by: {}",
                    authentication.getName());
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
                .orElseGet(() -> {
                    logger.warn("Attempt to authorize non-existent AppUser: {}", userId);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping("/{userId}/deauthorize")
    public ResponseEntity<?> deauthorizeUser(
            @PathVariable Long userId,
            Authentication authentication) {
        if (!isAdminUser(authentication)) {
            logger.warn("Unauthorized access attempt to deauthorize AppUser by: {}",
                    authentication.getName());
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
                .orElseGet(() -> {
                    logger.warn("Attempt to deauthorize non-existent AppUser: {}", userId);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String whatsappNumber,
            Authentication authentication) {
        if (!isAdminUser(authentication)) {
            logger.warn("Unauthorized access attempt to search users by: {}",
                    authentication.getName());
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

    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats(Authentication authentication) {
        if (!isAdminUser(authentication)) {
            logger.warn("Unauthorized access attempt to view AppUser stats by: {}",
                    authentication.getName());
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

    @PostMapping("/{userId}/link-whatsapp")
    public ResponseEntity<?> linkWhatsAppNumber(
            @PathVariable Long userId,
            @RequestParam String whatsappNumber,
            Authentication authentication) {
        if (!isAdminUser(authentication)) {
            logger.warn("Unauthorized access attempt to link WhatsApp by: {}",
                    authentication.getName());
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
                .orElseGet(() -> {
                    logger.warn("Attempt to link WhatsApp to non-existent user: {}", userId);
                    return ResponseEntity.notFound().build();
                });
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long userId,
            Authentication authentication) {
        if (!isAdminUser(authentication)) {
            logger.warn("Unauthorized access attempt to delete a user by the user: {}",
                    authentication.getName());
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