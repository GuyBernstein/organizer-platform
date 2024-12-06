package com.organizer.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.organizer.platform.model.ScraperDTO.ProcessingResult;
import com.organizer.platform.model.User.AppUser;
import com.organizer.platform.model.organizedDTO.MessageDTO;
import com.organizer.platform.model.organizedDTO.WhatsAppMessage;
import com.organizer.platform.service.Google.CloudStorageService;
import com.organizer.platform.service.Scraper.ContentProcessorService;
import com.organizer.platform.service.Scraper.WebContentScraperService;
import com.organizer.platform.service.User.UserService;
import com.organizer.platform.service.WhatsApp.WhatsAppMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

import static com.organizer.platform.controller.AppController.storedMediaName;
import static com.organizer.platform.model.User.AppUser.UserBuilder.anUser;
import static com.organizer.platform.model.organizedDTO.WhatsAppMessage.WhatsAppMessageBuilder.aWhatsAppMessage;

@Controller
@RequestMapping({"/"})
public class AuthController {
    private final UserService userService;
    private final WhatsAppMessageService messageService;
    private final WebContentScraperService scraperService;
    private final ObjectMapper objectMapper;
    private final JmsTemplate jmsTemplate;
    private final CloudStorageService cloudStorageService;

    @Autowired
    public AuthController(UserService userService, WhatsAppMessageService messageService,
                          WebContentScraperService scraperService, ObjectMapper objectMapper,
                          JmsTemplate jmsTemplate, CloudStorageService cloudStorageService) {
        this.userService = userService;
        this.messageService = messageService;
        this.scraperService = scraperService;
        this.objectMapper = objectMapper;
        this.jmsTemplate = jmsTemplate;
        this.cloudStorageService = cloudStorageService;
    }
//
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

    @GetMapping("/messages")
    public String messages(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }
        return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages");
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

    @PostMapping("/messages/delete")
    public String deleteMessage(@RequestParam Long messageId,
                                @AuthenticationPrincipal OAuth2User principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }

        try {
            messageService.deleteMessage(messageId);
            redirectAttributes.addFlashAttribute("successMessage", "ההודעה נמחקה בהצלחה");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "אירעה שגיאה במחיקת ההודעה");
        }

        return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages");
    }

    @PostMapping("/messages/update")
    public String editTextMessage(
            @RequestParam Long messageId,
            @RequestParam String type,
            @RequestParam String purpose,
            @RequestParam String messageContent,
            @RequestParam String tags,
            @RequestParam String nextSteps,
            @AuthenticationPrincipal OAuth2User principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }

        try {
            var message = messageService.findMessageById(messageId);
            if(message.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "אירעה שגיאה בעדכון ההודעה");
                return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages");
            }

            Set<String> commaSeparatedTags = Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());

            Set<String> commaSeparatedNextSteps = Arrays.stream(nextSteps.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());

            MessageDTO messageDTO = MessageDTO.builder()
                    .id(messageId)
                    .messageContent(messageContent)
                    .type(type)
                    .purpose(purpose)
                    .category(message.get().getCategory())
                    .subCategory(message.get().getSubCategory())
                    .tags(commaSeparatedTags)
                    .nextSteps(commaSeparatedNextSteps)
                    .build();

            messageService.partialUpdateMessage(message.get(), messageDTO);

            redirectAttributes.addFlashAttribute("successMessage", "ההודעה עודכנה");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "אירעה שגיאה בעדכון ההודעה");
        }

        return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages");
    }

    @PostMapping("/messages/smartUpdate")
    public String smartEditTextMessage(
            @RequestParam Long messageId,
            @RequestParam String messageContent,
            @AuthenticationPrincipal OAuth2User principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }

        try {
            var message = messageService.findMessageById(messageId);
            if(message.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "אירעה שגיאה בעדכון ההודעה");
                return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages");
            }

            // scrape url content, if any
            ContentProcessorService processor = new ContentProcessorService(scraperService);
            ProcessingResult result = processor.processContent(messageContent);

            // remove relations in the database from both sides
            messageService.deleteTags(message.get());
            messageService.deleteNextSteps(message.get());

            // set only the message content
            message.get().setMessageContent(result.getOriginalContent());
            // and purpose for possible url in content
            message.get().setPurpose(result.getScrapedContent());

            // Serialize the WhatsAppMessage to JSON string
            String serializedMessage = objectMapper.writeValueAsString(message.get());

            // Send the serialized JSON string to the queue for a reorganization of the message
            jmsTemplate.convertAndSend("exampleQueue", serializedMessage);


            redirectAttributes.addFlashAttribute("successMessage", "ההודעה נמצאת בעדכון");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "אירעה שגיאה בעדכון ההודעה");
        }

        return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages");
    }

    @PostMapping("/messages/text")
    public String createTextMessage(
            @RequestParam String content,
            @RequestParam String phoneNumber,
            @AuthenticationPrincipal OAuth2User principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }

        try {
            // Create and process the message
            ContentProcessorService processor = new ContentProcessorService(scraperService);
            ProcessingResult result = processor.processContent(content);

            WhatsAppMessage message = aWhatsAppMessage()
                    .fromNumber(phoneNumber)
                    .messageContent(result.getOriginalContent())
                    .messageType("text")
                    .processed(false)
                    .purpose(result.getScrapedContent())
                    .build();

            // Serialize and send to queue
            String serializedMessage = objectMapper.writeValueAsString(message);
            jmsTemplate.convertAndSend("exampleQueue", serializedMessage);

            redirectAttributes.addFlashAttribute("successMessage", "ההודעה נשלחה לעיבוד בהצלחה");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "אירעה שגיאה ביצירת ההודעה");
        }

        return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages");
    }

    @PostMapping("/messages/media")
    public String createMediaMessage(
            @RequestParam MultipartFile file,
            @RequestParam String phoneNumber,
            @AuthenticationPrincipal OAuth2User principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }

        try {
            String storedFileName;
            String metadata;

            String contentType = file.getContentType();
            if (contentType != null && contentType.startsWith("image/")) {
                // Handle image file
                storedFileName = cloudStorageService.uploadImage(phoneNumber,
                        file.getBytes(),
                        file.getContentType(),
                        file.getOriginalFilename()
                );
                metadata = storedMediaName("images/", file, storedFileName);
            } else {
                // Handle document file
                storedFileName = cloudStorageService.uploadDocument(phoneNumber,
                        file.getBytes(),
                        file.getContentType(),
                        file.getOriginalFilename()
                );
                metadata = storedMediaName("documents/", file, storedFileName);
            }


            // message creation
            WhatsAppMessage message = aWhatsAppMessage()
                    .fromNumber(phoneNumber)
                    .messageContent(metadata)
                    .messageType("image")
                    .processed(false)
                    .build();


            // Serialize and send to queue
            String serializedMessage = objectMapper.writeValueAsString(message);
            jmsTemplate.convertAndSend("exampleQueue", serializedMessage);

            redirectAttributes.addFlashAttribute("successMessage", "ההודעה נשלחה לעיבוד בהצלחה");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "אירעה שגיאה ביצירת ההודעה");
        }

        return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages");
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

        if(contentPage.equals("pages/messages")) {
            // Add the organized messages to the model
            Map<String, Map<String, List<MessageDTO>>> organizedMessages =
                    messageService.findMessageContentsByFromNumberGroupedByCategoryAndGroupedBySubCategory(appUser.getWhatsappNumber());
            model.addAttribute("categories", organizedMessages);
            long totalMessages = organizedMessages.values()    // Get all inner maps
                    .stream()
                    .flatMap(innerMap -> innerMap.values().stream())  // Get all lists
                    .mapToLong(List::size)              // Get all messages count
                    .sum();
            model.addAttribute("totalMessages", totalMessages);
            model.addAttribute("phone", appUser.getWhatsappNumber());
        }
    }

}
