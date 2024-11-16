package com.organizer.platform.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

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
    public String authStatus(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            logger.debug("User is not authenticated");
            return "Not authenticated";
        }
        logger.debug("User is authenticated: {}", principal.getName());
        return "Authenticated as: " + principal.getAttribute("email");
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
        logger.debug("Dashboard accessed by user: {}", principal.getName());
        model.addAttribute("name", principal.getAttribute("name"));
        model.addAttribute("email", principal.getAttribute("email"));
        return "dashboard";
    }
}
