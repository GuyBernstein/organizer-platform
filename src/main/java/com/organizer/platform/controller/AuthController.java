package com.organizer.platform.controller;

import com.organizer.platform.model.User.AppUser;
import com.organizer.platform.model.organizedDTO.MessageDTO;
import com.organizer.platform.service.User.UserService;
import com.organizer.platform.service.WhatsApp.WhatsAppMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.organizer.platform.model.User.AppUser.UserBuilder.anUser;

@Controller
@RequestMapping("/")
public class AuthController {

    private final UserService userService;
    private final WhatsAppMessageService messageService;
    @Autowired
    public AuthController(UserService userService, WhatsAppMessageService messageService) {
        this.userService = userService;
        this.messageService = messageService;
    }

    @GetMapping
    public String home(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/home");
        }
        return handleAuthorizedAccess(principal, model, "דף הבית", "pages/home");
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }
        return handleAuthorizedAccess(principal, model, "לוח בקרה", "pages/index");
    }

    @GetMapping("/categories")
    public String categories(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }
        return handleAuthorizedAccess(principal, model, "קטגוריות", "pages/categories");
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal OAuth2User principal,
                        @RequestParam(required = false) String logout,
                        Model model) {
        // If user is not authenticated, show login page
        if (principal == null) {
            if (logout != null) {
                model.addAttribute("logout", true);
            }
            return setupAnonymousPage(model, "התחברות", "pages/auth/login");
        }
        // show dashboard
        return handleAuthorizedAccess(principal, model, "לוח בקרה", "pages/index");
    }

    private String handleAuthorizedAccess(OAuth2User principal, Model model, String title, String contentPage) {
        String email = principal.getAttribute("email");
        Optional<AppUser> appUser = userService.findByEmail(email);

        if (appUser.isPresent() && appUser.get().isAuthorized()) {
            setupAuthorizedModel(model, principal, appUser.get(), title, contentPage);
        } else {
            setupUnauthorizedModel(model, principal,
                    appUser.orElse(anUser().authorized(false).build()));
        }
        return "layout/base";
    }

    private String setupAnonymousPage(Model model, String title, String contentPage) {
        model.addAttribute("title", title + " - Organizer Platform");
        model.addAttribute("content", contentPage);
        return "layout/base";
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

    private void setupAuthorizedModel(Model model, OAuth2User principal, AppUser appUser,
                                      String title, String contentPage) {
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");

        model.addAttribute("title", title + " - Organizer Platform");
        model.addAttribute("name", name != null ? name : "אורח");
        model.addAttribute("email", appUser.getEmail());
        model.addAttribute("picture", picture);
        model.addAttribute("content", contentPage);
        model.addAttribute("isAuthorized", appUser.isAuthorized());

        if(contentPage.equals("pages/categories")) {
            // Add the organized messages to the model
            Map<String, Map<String, List<MessageDTO>>> organizedMessages =
                    messageService.findMessageContentsByFromNumberGroupedByCategoryAndGroupedBySubCategory(appUser.getWhatsappNumber());
            model.addAttribute("categories", organizedMessages);
        }
    }

}
