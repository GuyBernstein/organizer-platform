package com.organizer.platform.controller;

import com.organizer.platform.model.User.AppUser;
import com.organizer.platform.model.User.UserRole;
import com.organizer.platform.service.User.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/auth-status")
    @ResponseBody
    public Map<String, Object> authStatus(@AuthenticationPrincipal OAuth2User principal) {
        Map<String, Object> response = new HashMap<>();

        if (principal == null) {
            response.put("status", "Not authenticated");
            return response;
        }
        String email = principal.getAttribute("email");
        response.put("oauth2_details", principal.getAttributes());

        // Check if user exists in our database
        Optional<AppUser> appUser = userService.findByEmail(email);
        response.put("user_exists", appUser.isPresent());
        appUser.ifPresent(user -> response.put("app_user_details", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "authorized", user.isAuthorized()
        )));

        return response;
    }

    @PostMapping("/ensure-user-created")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> ensureUserCreated(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Not authenticated"));
        }

        String email = principal.getAttribute("email");
        if (email == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No email found in OAuth2 details"));
        }

        Optional<AppUser> existingUser = userService.findByEmail(email);
        AppUser user = existingUser.orElseGet(() -> userService.save(AppUser.UserBuilder.anUser()
                .email(email)
                .role(UserRole.UNAUTHORIZED)
                .authorized(false)
                .build()));

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "user_id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "authorized", user.isAuthorized()
        ));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
        model.addAttribute("name", principal.getAttribute("name"));
        model.addAttribute("email", principal.getAttribute("email"));
        return "dashboard";
    }
}
