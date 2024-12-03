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
    public String login(@AuthenticationPrincipal OAuth2User principal,
                        @RequestParam(required = false) String unauthorized,
                        @RequestParam(required = false) String logout,
                        Model model) {
        // If user is not authenticated, show login page
        if (principal == null) {
            if (unauthorized != null) {
                model.addAttribute("unauthorized", true);
            }
            if (logout != null) {
                model.addAttribute("logout", true);
            }
            model.addAttribute("title", "התחברות - Organizer Platform");
            model.addAttribute("content", "pages/auth/login");
            return "layout/base";
        }

        // If user is authenticated, check authorization
        String email = principal.getAttribute("email");
        Optional<AppUser> appUser = userService.findByEmail(email);

        if (appUser.isPresent() && appUser.get().isAuthorized()) {
            // User is authorized, redirect to dashboard
            return "redirect:/dashboard";
        } else {
            // User is authenticated but not authorized
            setupUnauthorizedModel(model, principal, appUser.orElse(null));
            return "layout/base";
        }
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ROLE_USER') || hasAuthority('ROLE_ADMIN')")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
        String email = principal.getAttribute("email");
        try {
            AppUser appUser = userService.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            setupDashboardModel(model, principal, appUser);
        } catch (RuntimeException e){
            return "redirect:/login?error=true";
        }
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

    private void setupUnauthorizedModel(Model model, OAuth2User principal, AppUser appUser) {
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");
        String email = principal.getAttribute("email");

        model.addAttribute("title", "המתנה לאישור - Organizer Platform");
        model.addAttribute("name", name != null ? name : "אורח");
        model.addAttribute("email", email);
        model.addAttribute("picture", picture);
        model.addAttribute("content", "pages/auth/login");
        model.addAttribute("unauthorized", true);
        model.addAttribute("isAuthorized", appUser != null && appUser.isAuthorized());
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
