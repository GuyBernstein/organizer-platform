package com.organizer.platform.controller;

import com.organizer.platform.util.AccessControlResponse;
import com.organizer.platform.model.User.AppUser;
import com.organizer.platform.model.organizedDTO.MessageDTO;
import com.organizer.platform.model.organizedDTO.WhatsAppMessage;
import com.organizer.platform.service.Google.CloudStorageService;
import com.organizer.platform.service.User.UserService;
import com.organizer.platform.service.WhatsApp.WhatsAppMessageService;
import com.organizer.platform.util.PhoneNumberValidator;
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

import javax.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.organizer.platform.util.PhoneNumberValidator.validateAndCheckAccess;
import static com.organizer.platform.util.PhoneNumberValidator.validatePhoneNumber;

@RestController
@RequestMapping("/api/content")
@Api(tags = "Content Management API")
public class AppController {
    private static final Logger log = LoggerFactory.getLogger(AppController.class);
    private final WhatsAppMessageService messageService;
    private final CloudStorageService cloudStorageService;
    private final UserService userService;

    @Autowired
    public AppController(WhatsAppMessageService messageService, CloudStorageService cloudStorageService, UserService userService) {
        this.messageService = messageService;
        this.cloudStorageService = cloudStorageService;
        this.userService = userService;
    }

    @GetMapping("/{messageId}/related")
    @ApiOperation(value = "Get related messages by shared tags",
            notes = "Retrieves all messages that share tags with the specified message, organized by category and subcategory")
    public ResponseEntity<?> getRelatedMessages(
            @PathVariable Long messageId,
            @RequestParam(required = false, defaultValue = "1") int minimumSharedTags,
            Authentication authentication) {

        // Validate message access
        WhatsAppMessage message = messageService.findMessageById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));

        // Check access control
        AccessControlResponse accessControl = checkAccessControl(authentication, message.getFromNumber());
        if (!accessControl.isAllowed()) {
            log.warn("Unauthorized access attempt to related messages for number: {} by user: {} - {}",
                    message.getFromNumber(), authentication.getName(), accessControl.getMessage());
            return accessControl.toResponseEntity();
        }

        Map<String, Map<String, List<MessageDTO>>> relatedMessages;
        relatedMessages = messageService.findRelatedMessagesWithMinimumSharedTags(message, minimumSharedTags);

        if (relatedMessages.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(relatedMessages, HttpStatus.OK);
    }

    @GetMapping("/messages/{phoneNumber}")
    @ApiOperation(value = "Get message contents by phone number",
            notes = "Retrieves all message contents sent from a specific phone number. Accepts formats: 0509603888 or 972509603888")
    public ResponseEntity<?> getMessageContentsByPhoneNumber(
            @PathVariable String phoneNumber,
            Authentication authentication) {

        // Validate input phone number format and convert to international format
        ResponseEntity<?> validationResponse = validateAndCheckAccess(
                phoneNumber,
                authentication,
                this::checkAccessControl
        );

        if (validationResponse != null) {
            return validationResponse;
        }

        // Get the validated international format
        String internationalFormat = validatePhoneNumber(phoneNumber)
                .getInternationalFormat();


        Map<String, Map<String, List<MessageDTO>>>  organizedMessages =
                messageService.findMessageContentsByFromNumberGroupedByCategoryAndGroupedBySubCategory(internationalFormat);

        if (organizedMessages.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return new ResponseEntity<>(organizedMessages, HttpStatus.OK);
    }

    @GetMapping("/image/url")
    public ResponseEntity<?> getImagePreSignedUrl(
            @RequestParam String imageName,
            @RequestParam String phoneNumber,
            Authentication authentication) {

        ResponseEntity<?> validationResponse = PhoneNumberValidator.validateAndCheckAccess(
                phoneNumber,
                authentication,
                this::checkAccessControl
        );

        if (validationResponse != null) {
            return validationResponse;
        }

        String internationalFormat = PhoneNumberValidator.validatePhoneNumber(phoneNumber)
                .getInternationalFormat();

        try {
            String preSignedUrl = cloudStorageService.generateImageSignedUrl(internationalFormat, imageName);
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
            Authentication authentication) {

        // Validate input phone number format and convert to international format
        ResponseEntity<?> validationResponse = PhoneNumberValidator.validateAndCheckAccess(
                phoneNumber,
                authentication,
                this::checkAccessControl
        );

        if (validationResponse != null) {
            return validationResponse;
        }

        String internationalFormat = PhoneNumberValidator.validatePhoneNumber(phoneNumber)
                .getInternationalFormat();

        try {
            // Generate pre-signed URL for the document
            String preSignedUrl = cloudStorageService.generateDocumentSignedUrl(internationalFormat, documentName);

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
            Authentication authentication) {


        // Validate input phone number format and convert to international format
        ResponseEntity<?> validationResponse = PhoneNumberValidator.validateAndCheckAccess(
                phoneNumber,
                authentication,
                this::checkAccessControl
        );

        if (validationResponse != null) {
            return validationResponse;
        }

        String internationalFormat = PhoneNumberValidator.validatePhoneNumber(phoneNumber)
                .getInternationalFormat();

        try {
            // Generate pre-signed URL for the audio file
            String preSignedUrl = cloudStorageService.generateAudioSignedUrl(internationalFormat, audioName);

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
    public ResponseEntity<?> deleteAll(Authentication authentication) {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        if (!userService.isAdmin(email)) {
            AccessControlResponse accessControl = AccessControlResponse.denied(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required",
                    "Only admin can access this content"
            );
            log.warn("Unauthorized deletion attempt by user: {}", email);
            return accessControl.toResponseEntity();
        }
        messageService.cleanDatabasePostgres();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/messages/{messageId}")
    @ApiOperation(value = "Update message content and metadata",
            notes = "Updates only the provided non-null fields of the message, preserving existing values for null fields")
    public ResponseEntity<?> updateMessage(
            @PathVariable Long messageId,
            @RequestBody(required = false) MessageDTO updateRequest,
            Authentication authentication) {

        if (updateRequest == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Update request cannot be null"));
        }

        try {
            // Single message retrieval that will be used by both access control and update
            WhatsAppMessage message = messageService.findMessageById(messageId)
                    .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));

            // Check access control
            AccessControlResponse accessControl = checkAccessControl(authentication, message.getFromNumber());
            if (!accessControl.isAllowed()) {
                log.warn("Unauthorized update attempt for message: {} by user: {} - {}",
                        messageId, authentication.getName(), accessControl.getMessage());
                return accessControl.toResponseEntity();
            }

            return ResponseEntity.ok(messageService.partialUpdateMessage(message, updateRequest));
        } catch (EntityNotFoundException e) {
            log.warn("Message not found with id: {}", messageId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating message with id: {}", messageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update message: " + e.getMessage()));
        }
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

    private AccessControlResponse checkAccessControl(Authentication authentication, String phoneNumber) {
        // If no authentication, deny access
        if (authentication == null) {
            return AccessControlResponse.denied(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required",
                    "Please log in to access this content"
            );
        }

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");

        // Admin can access all content
        if (userService.isAdmin(email)) {
            return AccessControlResponse.allowed();
        }

        // For regular users, check if they are authorized and the phone number matches their account
        Optional<AppUser> user = userService.findByEmail(email);

        if (user.isEmpty()) {
            return AccessControlResponse.denied(
                    HttpStatus.FORBIDDEN,
                    "User not found",
                    "No user account found for your email address"
            );
        }

        AppUser appUser = user.get();

        if (!appUser.isAuthorized()) {
            return AccessControlResponse.denied(
                    HttpStatus.FORBIDDEN,
                    "Account not authorized",
                    "Your account is pending authorization. Please contact an administrator"
            );
        }

        if (appUser.getWhatsappNumber() == null) {
            return AccessControlResponse.denied(
                    HttpStatus.FORBIDDEN,
                    "No WhatsApp number linked",
                    "Your account does not have a linked WhatsApp number"
            );
        }

        boolean numberMatches = appUser.getWhatsappNumber().equals(phoneNumber) ||
                appUser.getWhatsappNumber().equals("972" + phoneNumber.substring(1));

        if (!numberMatches) {
            return AccessControlResponse.denied(
                    HttpStatus.FORBIDDEN,
                    "Unauthorized access",
                    "You can only access content associated with your own number: " + appUser.getWhatsappNumber()
            );
        }

        return AccessControlResponse.allowed();
    }
}