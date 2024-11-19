package com.organizer.platform.controller;

import com.organizer.platform.model.User.AppUser;
import com.organizer.platform.service.Google.CloudStorageService;
import com.organizer.platform.service.User.UserService;
import com.organizer.platform.service.WhatsApp.WhatsAppImageService;
import com.organizer.platform.service.WhatsApp.WhatsAppMessageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/content")
@Api(tags = "Content Management API")
public class AppController {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppImageService.class);
    private final WhatsAppMessageService messageService;
    private final CloudStorageService cloudStorageService;
    private final UserService userService;

    @Autowired
    public AppController(WhatsAppMessageService messageService, CloudStorageService cloudStorageService, UserService userService) {
        this.messageService = messageService;
        this.cloudStorageService = cloudStorageService;
        this.userService = userService;
    }

    private boolean canAccessContent(Authentication authentication, String phoneNumber) {
        if (authentication == null)
            return true;

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");

        // Admin can access all content
        if (userService.isAdmin(email)) {
            return false;
        }

        // For regular users, check if the phone number matches their account
        Optional<AppUser> user = userService.findByEmail(email);
        return user.map(appUser ->
                appUser.isAuthorized() &&
                        appUser.getWhatsappNumber() != null &&
                        (appUser.getWhatsappNumber().equals(phoneNumber) ||
                                appUser.getWhatsappNumber().equals("972" + phoneNumber.substring(1)))
        ).orElse(false);
    }


    @GetMapping("/messages/{phoneNumber}")
    @ApiOperation(value = "Get message contents by phone number",
            notes = "Retrieves all message contents sent from a specific phone number. Accepts formats: 0509603888 or 972509603888")
    public ResponseEntity<Map<String, List<String>>> getMessageContentsByPhoneNumber(
            @PathVariable String phoneNumber,
            Authentication authentication) {

        // Validate input phone number format and convert to international format
        String internationalFormat;

        // Check if phone starts with 0 (0509603888)
        if (phoneNumber.matches("^05\\d{8}$")) {
            internationalFormat = "972" + phoneNumber.substring(1);
        }
        // Check if phone starts with 972 (972509603888)
        else if (phoneNumber.matches("^972\\d{9}$")) {
            internationalFormat = phoneNumber;
        }
        // Invalid format
        else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        // Check if user has permission to access this content
        if (canAccessContent(authentication, phoneNumber)) {
            log.warn("Unauthorized access attempt to messages for number: {} by user: {}",
                    phoneNumber, authentication.getName());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        Map<String, List<String>> messageContentsByCategory =
                messageService.findMessageContentsByFromNumberGroupedByCategory(internationalFormat);

        if (messageContentsByCategory.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(messageContentsByCategory, HttpStatus.OK);
    }

    @GetMapping("/image/url")
    public ResponseEntity<?> getImagePreSignedUrl(
            @RequestParam String imageName,
            @RequestParam String phoneNumber,
            Authentication authentication,
            HttpServletRequest request) {

        log.info("Request received: {} {}", request.getMethod(), request.getRequestURI());
        log.info("ImageName received: {}", imageName);

        if (canAccessContent(authentication, phoneNumber)) {
            log.warn("Unauthorized access attempt to image: {} by user: {}",
                    imageName, authentication.getName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Unauthorized access"));
        }

        try {
            String preSignedUrl = cloudStorageService.generateSignedUrl(imageName);
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", preSignedUrl);
            response.put("fileName", imageName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/document/url")
    public ResponseEntity<?> getDocumentPreSignedUrl(
            @RequestParam() String documentName,
            @RequestParam String phoneNumber,
            Authentication authentication,
            HttpServletRequest request) {


        log.info("Request received: {} {}", request.getMethod(), request.getRequestURI());
        log.info("DocumentName received: {}", documentName);

        if (canAccessContent(authentication, phoneNumber)) {
            log.warn("Unauthorized access attempt to document: {} by user: {}",
                    documentName, authentication.getName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Unauthorized access"));
        }

        try {
            // Generate pre-signed URL for the document
            String preSignedUrl = cloudStorageService.generateDocumentSignedUrl(documentName);

            // Create response with additional metadata
            Map<String, String> response = new HashMap<>();
            response.put("documentUrl", preSignedUrl);
            response.put("fileName", documentName);

            // Extract file extension for content type hints
            String fileExtension = getFileExtension(documentName);
            if (fileExtension != null) {
                response.put("fileType", fileExtension);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating document URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/audio/url")
    @ApiOperation(value = "Get pre-signed URL for audio file",
            notes = "Retrieves a pre-signed URL for accessing an audio file stored in Cloud Storage")
    public ResponseEntity<?> getAudioPreSignedUrl(
            @RequestParam String audioName,
            @RequestParam String phoneNumber,
            Authentication authentication,
            HttpServletRequest request) {

        log.info("Request received: {} {}", request.getMethod(), request.getRequestURI());
        log.info("AudioName received: {}", audioName);

        if (canAccessContent(authentication, phoneNumber)) {
            log.warn("Unauthorized access attempt to audio: {} by user: {}",
                    audioName, authentication.getName());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Unauthorized access"));
        }

        try {
            // Generate pre-signed URL for the audio file
            String preSignedUrl = cloudStorageService.generateAudioSignedUrl(audioName);

            // Create response with metadata
            Map<String, String> response = new HashMap<>();
            response.put("audioUrl", preSignedUrl);
            response.put("fileName", audioName);

            // Add audio file type information
            String fileExtension = getFileExtension(audioName);
            if (fileExtension != null) {
                response.put("fileType", fileExtension);

                // Add content type hint based on file extension
                String contentType = getAudioContentType(fileExtension);
                if (contentType != null) {
                    response.put("contentType", contentType);
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating audio URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAll(Authentication authentication) {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        if (!userService.isAdmin(email)) {
            log.warn("Unauthorized deletion attempt by user: {}", email);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        messageService.deleteAllMessages();
        return ResponseEntity.ok().build();
    }

    private String getAudioContentType(String fileExtension) {
        if (fileExtension == null) return null;
        switch (fileExtension.toLowerCase())
        {
            case "ogg":
                return "audio/ogg";
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "m4a":
                return "audio/mp4";
            case "aac":
                return "audio/aac";
            case "webm":
                return "audio/webm";
            default:
                return null;
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return null;
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : null;
    }
}