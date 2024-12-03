package com.organizer.platform.controller;

import com.organizer.platform.model.User.AppUser;
import com.organizer.platform.model.User.UserRole;
import com.organizer.platform.service.User.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
@Controller
@RequestMapping("/")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String home(Model model) {
        model.addAttribute("title", "דף הבית - Organizer Platform");
        model.addAttribute("content", "pages/home");
        return "layout/base";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) boolean unauthorized,
                        Model model) {
        if (unauthorized) {
            model.addAttribute("unauthorized", true);
        }
        model.addAttribute("title", "המתנה לאישור - Organizer Platform");
        model.addAttribute("content", "pages/auth/login");
        return "layout/base";
    }

    // This method will handle the OAuth2 callback
    @GetMapping("/oauth2/callback/google")
    public String handleGoogleCallback(@AuthenticationPrincipal OAuth2User principal) {
        String email = principal.getAttribute("email");

        if (email == null) {
            return "redirect:/login?error";
        }

        AppUser appUser = userService.findByEmail(email)
                .orElseGet(() -> {
                    // Create new user if they don't exist
                    AppUser newUser = AppUser.UserBuilder.anUser()
                            .email(email)
                            .role(UserRole.UNAUTHORIZED)
                            .authorized(false)
                            .build();
                    return userService.save(newUser);
                    // New user created, redirect to unauthorized page
                });

        if (!appUser.isAuthorized()) {
            return "redirect:/login?unauthorized=true";
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
        String email = principal.getAttribute("email");

        if (email == null) {
            return "redirect:/";
        }

        // Ensure user exists in our system
        Optional<AppUser> appUser = userService.findByEmail(email);
        if (appUser.isEmpty()) {
            // Create new user if they don't exist
            AppUser newUser = AppUser.UserBuilder.anUser()
                    .email(email)
                    .role(UserRole.UNAUTHORIZED)
                    .authorized(false)
                    .build();
            userService.save(newUser);
            return "redirect:/login";
        }

        // Check if user is authorized
        if (!appUser.get().isAuthorized()) {
            return "redirect:/login";
        }

        // User exists and is authorized, proceed to dashboard
        setupDashboardModel(model, principal, appUser.get());
        return "layout/base";
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

    private void setupDashboardModel(Model model, OAuth2User principal, AppUser appUser) {
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");

        model.addAttribute("title", "לוח בקרה - Organizer Platform");
        model.addAttribute("name", name != null ? name : "אורח");
        model.addAttribute("email", appUser.getEmail());
        model.addAttribute("picture", picture);
        model.addAttribute("content", "pages/dashboard/index");
        model.addAttribute("isAuthorized", appUser.isAuthorized());
    }
}
