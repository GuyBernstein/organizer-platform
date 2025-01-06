package com.organizer.platform.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.organizer.platform.util.AccessControlResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.*;

import static com.organizer.platform.model.organizedDTO.WhatsAppMessage.WhatsAppMessageBuilder.aWhatsAppMessage;
import static com.organizer.platform.util.PhoneNumberValidator.validateAndCheckAccess;
import static com.organizer.platform.util.PhoneNumberValidator.validatePhoneNumber;

/**
 * Controller class handling all content management operations for the platform.
 * This class provides RESTful endpoints for managing WhatsApp messages, files,
 * and related content processing operations.
 * <p>
 * Key features:
 * - Message management and organization
 * - File handling (images, documents, audio)
 * - Content processing and categorization
 * - Access control and user authentication
 * - Related content discovery
 * <p>
 * All endpoints require OAuth2 authentication and appropriate user authorization.
 */
@RestController
@RequestMapping("/api/content")
@Api(tags = "Content Management API")
public class AppController {

    private final WhatsAppMessageService messageService;
    private final CloudStorageService cloudStorageService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final JmsTemplate jmsTemplate;
    private final WebContentScraperService scraperService;


    @Autowired
    public AppController(WhatsAppMessageService messageService, CloudStorageService cloudStorageService, UserService userService, ObjectMapper objectMapper, JmsTemplate jmsTemplate, WebContentScraperService scraperService) {
        this.messageService = messageService;
        this.cloudStorageService = cloudStorageService;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.jmsTemplate = jmsTemplate;
        this.scraperService = scraperService;
    }

    /**
     * Deletes all messages from the database. This operation is restricted to admin users only.
     *
     * @param authentication The OAuth2 authentication object containing user credentials
     * @return ResponseEntity<?> Returns:
     *         - 200 OK if deletion is successful
     *         - 401 UNAUTHORIZED if user is not authenticated
     *         - 403 FORBIDDEN if user is not an admin
     *
     * @apiNote This is a dangerous operation that should be used with caution
     * @see UserService#isAdmin(String)
     */
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
            return accessControl.toResponseEntity();
        }
        messageService.cleanDatabasePostgres();
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves messages that share tags with a specified message, organized by category
     * and subcategory. Messages are considered related if they share at least the specified
     * minimum number of tags.
     *
     * @param messageId The ID of the reference message to find related content for
     * @param minimumSharedTags Minimum number of shared tags required for messages to be considered related (default: 1)
     * @param authentication The OAuth2 authentication object containing user credentials
     * @return ResponseEntity<?> Returns:
     *         - 200 OK with map of related messages grouped by category and subcategory
     *         - 204 NO_CONTENT if no related messages are found
     *         - 401 UNAUTHORIZED if user is not authenticated
     *         - 403 FORBIDDEN if user doesn't have access to the message
     *         - 404 NOT_FOUND if the reference message doesn't exist
     *
     * @throws EntityNotFoundException if the specified message is not found
     * @see WhatsAppMessageService#findRelatedMessagesWithMinimumSharedTags
     */
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
            return accessControl.toResponseEntity();
        }

        Map<String, Map<String, List<MessageDTO>>> relatedMessages;
        relatedMessages = messageService.findRelatedMessagesWithMinimumSharedTags(message, minimumSharedTags);

        if (relatedMessages.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(relatedMessages, HttpStatus.OK);
    }

    /**
     * Retrieves and organizes messages by phone number while ensuring proper access control.
     * <p>
     * Why this implementation:
     * - Phone number validation is critical as it's the primary key for message retrieval and security
     * - Messages are organized by category and subcategory to provide a structured view of user's content
     *   which helps in better content navigation and organization
     * - Uses access control checking to ensure users can only access their own messages, maintaining
     *   data privacy and security
     * - Returns empty response instead of error for no content to simplify client-side handling
     * - Supports both local and international phone number formats to improve user experience
     */
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

    /**
     * Generates temporary secure URLs for accessing images stored in cloud storage.
     * <p>
     * Why this implementation:
     * - Uses pre-signed URLs to provide temporary, secure access to private cloud storage
     *   without exposing permanent public URLs
     * - Includes both URL and filename in response to help clients with display and download functionality
     * - Implements phone number validation to maintain data isolation between users
     * - Returns detailed error messages to help debug cloud storage access issues
     * - Access control ensures users can only access images associated with their phone number
     *   even if they know the image name
     */
    @GetMapping("/image/url")
    public ResponseEntity<?> getImagePreSignedUrl(
            @RequestParam String imageName,
            @RequestParam String phoneNumber,
            Authentication authentication) {

        ResponseEntity<?> validationResponse = validateAndCheckAccess(
                phoneNumber,
                authentication,
                this::checkAccessControl
        );

        if (validationResponse != null) {
            return validationResponse;
        }

        String internationalFormat = validatePhoneNumber(phoneNumber)
                .getInternationalFormat();

        try {
            String preSignedUrl = cloudStorageService.generateImageSignedUrl(internationalFormat, imageName);
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", preSignedUrl);
            response.put("fileName", imageName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generates a pre-signed URL for secure document access while maintaining data privacy and security.
     * <p>
     * Why this method exists:
     * 1. Security & Privacy: Instead of exposing direct document URLs, pre-signed URLs provide temporary,
     *    authenticated access to documents stored in cloud storage.
     * 2. Access Control: Ensures documents are only accessible by authorized users who own the associated
     *    phone number or have admin privileges.
     * 3. User Experience: Provides additional metadata like file type to help frontend applications
     *    handle different document types appropriately.
     * 4. Resource Management: By generating temporary URLs, we maintain control over document access
     *    while allowing direct downloads from cloud storage, reducing server load.
     *
     * @param documentName Name of the document to access
     * @param phoneNumber Associated phone number for access control
     * @param authentication Current user's authentication details
     * @return ResponseEntity containing the pre-signed URL and metadata, or error details
     */
    @GetMapping("/document/url")
    public ResponseEntity<?> getDocumentPreSignedUrl(
            @RequestParam() String documentName,
            @RequestParam String phoneNumber,
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

        String internationalFormat = validatePhoneNumber(phoneNumber)
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Generates a pre-signed URL for secure audio file access with media-specific handling.
     * <p>
     * Why this method exists:
     * 1. Media Streaming Support: Provides content-type information necessary for proper audio
     *    playback in web browsers and mobile applications.
     * 2. Bandwidth Optimization: Direct cloud storage access allows for efficient streaming and
     *    download of potentially large audio files without proxying through the application server.
     * 3. Format Compatibility: Includes audio-specific metadata and content types to help client
     *    applications choose appropriate playback methods and codecs.
     * 4. Security & Privacy: Protects sensitive audio content through temporary, authenticated
     *    access while maintaining user privacy.
     * 5. Access Control Integration: Ensures audio files are only accessible by the message owner
     *    or administrators, protecting user privacy.
     *
     * @param audioName Name of the audio file to access
     * @param phoneNumber Associated phone number for access control
     * @param authentication Current user's authentication details
     * @return ResponseEntity containing the pre-signed URL and audio metadata, or error details
     */
    @GetMapping("/audio/url")
    @ApiOperation(value = "Get pre-signed URL for audio file",
            notes = "Retrieves a pre-signed URL for accessing an audio file stored in Cloud Storage")
    public ResponseEntity<?> getAudioPreSignedUrl(
            @RequestParam String audioName,
            @RequestParam String phoneNumber,
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

        String internationalFormat = validatePhoneNumber(phoneNumber)
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Provides a traditional update mechanism for WhatsApp messages that preserves existing data structure.
     * <p>
     * Why this method exists:
     * - Allows for selective updates without requiring all fields to be present
     * - Maintains data integrity by preserving existing relationships and metadata
     * - Useful for simple content updates where AI reprocessing isn't needed
     * - Provides immediate feedback as updates are processed synchronously
     * <p>
     * This approach is ideal when:
     * - Only specific fields need to be updated
     * - The existing tags and relationships should be preserved
     * - Real-time confirmation of the update is required
     * - The update doesn't warrant re-analysis of content
     */
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
                return accessControl.toResponseEntity();
            }

            return ResponseEntity.ok(messageService.partialUpdateMessage(message, updateRequest));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update message: " + e.getMessage()));
        }
    }

    /**
     * Provides an AI-driven update mechanism that completely reprocesses the message content.
     * <p>
     * Why this method exists:
     * - Enables complete content reanalysis when message meaning/context changes significantly
     * - Maintains data freshness by regenerating all AI-derived fields
     * - Ensures consistency between content and its metadata through complete reprocessing
     * - Handles content updates that may affect related items or categorization
     * <p>
     * This approach is ideal when:
     * - The content change affects the message's meaning or context
     * - Tags and relationships need to be recalculated
     * - URLs in the content need fresh scraping
     * - Asynchronous processing is acceptable (uses JMS queue)
     * <p>
     * Key differences from regular update:
     * - Deletes existing tags and relationships instead of preserving them
     * - Re-scrapes URLs and updates purpose field
     * - Processes updates asynchronously through message queue
     * - Regenerates all AI-derived fields rather than updating selectively
     */
    @PutMapping("/messages/{messageId}/content")
    @ApiOperation(value = "Smart Update message content and metadata",
            notes = "Updates only the provided content of the message, while regenerating values for other fields")
    public ResponseEntity<?> SmartUpdateMessage(
            @PathVariable Long messageId,
            @RequestParam String content,
            Authentication authentication) {

        if (content == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Smart Update's content cannot be null"));
        }

        try {
            // Single message retrieval that will be used by both access control and update
            WhatsAppMessage message = messageService.findMessageById(messageId)
                    .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));

            // Check access control
            AccessControlResponse accessControl = checkAccessControl(authentication, message.getFromNumber());
            if (!accessControl.isAllowed()) {
                return accessControl.toResponseEntity();
            }

            // remove relations in the database from both sides
            messageService.deleteTags(message);
            messageService.deleteNextSteps(message);

            // scrape url content, if any
            ContentProcessorService processor = new ContentProcessorService(scraperService);
            ProcessingResult result = processor.processContent(content);

            // set only the message content
            message.setMessageContent(result.getOriginalContent());
            // and purpose for possible url in content
            message.setPurpose(result.getScrapedContent());

            // Serialize the WhatsAppMessage to JSON string
            String serializedMessage = objectMapper.writeValueAsString(message);

            // Send the serialized JSON string to the queue for a reorganization of the message
            jmsTemplate.convertAndSend("exampleQueue", serializedMessage);

            return ResponseEntity.ok()
                    .body(Map.of("processing", "updating message: " + serializedMessage));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing message", e);
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update message: " + e.getMessage()));
        }
    }

    /**
     * Creates a new text-based message entry from user input while ensuring proper organization and processing.
     * <p>
     * Why this method exists:
     * - Provides a streamlined way to create text entries without requiring full message object creation
     * - Handles automatic URL content extraction to enrich message metadata
     * - Ensures proper access control and phone number validation before processing
     * - Delegates heavy processing to background queue to maintain responsive API
     * <p>
     * Processing flow:
     * 1. Validates user access and phone number format
     * 2. Extracts and processes any URLs in the content to gather additional context
     * 3. Creates a minimal message object with essential fields
     * 4. Offloads organization and classification to async queue for better performance
     *
     * @param content Raw text content to be processed and organized
     * @param phoneNumber User's phone number for authentication and message association
     * @param authentication User's authentication details for access control
     * @return Response entity containing processing status or error details
     */
    @PostMapping("/messages/text/")
    @ApiOperation(value = "Create an organized content from text",
            notes = "Get input only the provided text content of the message, while generating values for other fields")
    public ResponseEntity<?> createTextMessage(
            @RequestParam String content,
            @RequestParam String phoneNumber,
            Authentication authentication) {

        if (content == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "For creating message, text content cannot be null"));
        }

        // Validate input phone number format and convert to international format
        ResponseEntity<?> validationResponse = validateAndCheckAccess(
                phoneNumber,
                authentication,
                this::checkAccessControl
        );

        if (validationResponse != null) {
            return validationResponse;
        }

        String internationalFormat = validatePhoneNumber(phoneNumber)
                .getInternationalFormat();

        // scrape url content, if any
        ContentProcessorService processor = new ContentProcessorService(scraperService);
        ProcessingResult result = processor.processContent(content);

        // message creation
        WhatsAppMessage message = aWhatsAppMessage()
                .fromNumber(internationalFormat)
                .messageContent(result.getOriginalContent())
                .messageType("text")
                .processed(false)
                .purpose(result.getScrapedContent()) // should be empty if not scrapable or no url included
                .build();

        try {
            // Check access control
            AccessControlResponse accessControl = checkAccessControl(authentication, internationalFormat);
            if (!accessControl.isAllowed()) {
                return accessControl.toResponseEntity();
            }

            // Serialize the WhatsAppMessage to JSON string
            String serializedMessage = objectMapper.writeValueAsString(message);

            // Send the serialized JSON string to the queue for a reorganization of the message
            jmsTemplate.convertAndSend("exampleQueue", serializedMessage);

            return ResponseEntity.ok()
                    .body(Map.of("Processing...", "Creating message: " + serializedMessage));

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing message", e);
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create message: " + e.getMessage()));
        }
    }

    /**
     * Handles image upload and creates a new message entry with proper storage and metadata.
     * <p>
     * Why this method exists:
     * - Provides secure and organized storage of user-uploaded images
     * - Ensures proper file type validation to maintain system integrity
     * - Creates standardized metadata for consistent image retrieval
     * - Integrates with cloud storage for scalable image management
     * <p>
     * Processing flow:
     * 1. Validates image file format and user access rights
     * 2. Uploads image to cloud storage with proper organization
     * 3. Creates metadata including MIME type, size, and storage location
     * 4. Initiates async processing for message organization
     * <p>
     * The method uses cloud storage instead of direct database storage because:
     * - Enables efficient handling of large files
     * - Provides better scalability for growing storage needs
     * - Allows for secure access control through pre-signed URLs
     * - Reduces database load and storage costs
     *
     * @param image MultipartFile containing the image to be uploaded
     * @param phoneNumber User's phone number for authentication and message association
     * @param authentication User's authentication details for access control
     * @return Response entity containing processing status or error details
     * @throws IOException If there are issues handling the file upload
     */
    @PostMapping("/messages/image")
    @ApiOperation(value = "Create message with image content and metadata",
            notes = "Handles image file upload and creates a WhatsApp message with image content")
    public ResponseEntity<?> createImageMessage(
            @RequestParam("image") MultipartFile image,
            @RequestParam String phoneNumber,
            Authentication authentication) throws IOException {

        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Image file is required"));
        }

        // Validate file type
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid file type. Only images are allowed"));
        }

        // Validate input phone number format and convert to international format
        ResponseEntity<?> validationResponse = validateAndCheckAccess(
                phoneNumber,
                authentication,
                this::checkAccessControl
        );

        if (validationResponse != null) {
            return validationResponse;
        }

        String internationalFormat = validatePhoneNumber(phoneNumber)
                .getInternationalFormat();


        String storedFileName = cloudStorageService.uploadImage(internationalFormat,
                image.getBytes(),
                image.getContentType(),
                image.getOriginalFilename()
        );
        String imageMetadata = storedMediaName("images/", image, storedFileName);

        // message creation
        WhatsAppMessage message = aWhatsAppMessage()
                .fromNumber(internationalFormat)
                .messageContent(imageMetadata)
                .messageType("image")
                .processed(false)
                .build();

        return sendToJMS(message);
    }

    /**
     * Handles document upload requests by validating, storing, and queuing them for processing.
     * <p>
     * Why this exists:
     * - Provides a standardized entry point for document uploads that ensures:
     *   1. Security: Validates file types to prevent malicious uploads
     *   2. Access Control: Ensures users can only upload to their own accounts
     *   3. Consistency: Maintains a uniform document storage structure in cloud storage
     * <p>
     * Design decisions:
     * - Uses MultipartFile to handle large document uploads efficiently
     * - Separates storage (immediate) from processing (async via JMS) to improve responsiveness
     * - Enforces strict MIME type validation to maintain system security and data integrity
     */
    @PostMapping("/messages/document")
    @ApiOperation(value = "Create message with document content and metadata",
            notes = "Handles document file upload and creates a WhatsApp message with document content")
    public ResponseEntity<?> createDocumentMessage(
            @RequestParam("document") MultipartFile document,
            @RequestParam String phoneNumber,
            Authentication authentication) throws IOException {

        if (document == null || document.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Document file is required"));
        }

        // Validate file type
        String contentType = document.getContentType();
        if (contentType == null || !isValidDocumentType(contentType)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid file type. Supported formats: PDF, DOC, DOCX, TXT, XLS, XLSX"));
        }

        // Validate input phone number format and convert to international format
        ResponseEntity<?> validationResponse = validateAndCheckAccess(
                phoneNumber,
                authentication,
                this::checkAccessControl
        );

        if (validationResponse != null) {
            return validationResponse;
        }

        String internationalFormat = validatePhoneNumber(phoneNumber)
                .getInternationalFormat();


        String storedFileName = cloudStorageService.uploadDocument(internationalFormat,
                document.getBytes(),
                document.getContentType(),
                document.getOriginalFilename()
        );

        String documentMetadata = storedMediaName("documents/", document, storedFileName);

        // message creation
        WhatsAppMessage message = aWhatsAppMessage()
                .fromNumber(internationalFormat)
                .messageContent(documentMetadata)
                .messageType("document")
                .processed(false)
                .build();

        return sendToJMS(message);
    }

    /**
     * Handles asynchronous message processing by serializing and sending messages to JMS queue.
     * <p>
     * Why this exists:
     * - Decouples message creation from processing to:
     *   1. Improve system responsiveness by handling heavy processing asynchronously
     *   2. Enable better scalability by distributing processing load
     *   3. Provide resilience through message persistence and retry capabilities
     * <p>
     * Design decisions:
     * - Uses JSON serialization for maximum compatibility and flexibility
     * - Returns immediate feedback while processing continues asynchronously
     * - Centralizes JMS sending logic to maintain consistent error handling
     */
    private ResponseEntity<?> sendToJMS(WhatsAppMessage message) {
        try {
            // Serialize the WhatsAppMessage to JSON string
            String serializedMessage = objectMapper.writeValueAsString(message);

            // Send the serialized JSON string to the queue for a reorganization of the message
            jmsTemplate.convertAndSend("exampleQueue", serializedMessage);

            return ResponseEntity.ok()
                    .body(Map.of("Processing...", "Creating message: " + serializedMessage));

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing message", e);
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create message: " + e.getMessage()));
        }
    }

    /**
     * Generates standardized metadata string for stored media files.
     * <p>
     * Why this exists:
     * - Provides a consistent way to:
     *   1. Track file metadata across the system
     *   2. Clean up storage path prefixes for better readability
     *   3. Maintain uniform media information format for all stored files
     * <p>
     * Design decisions:
     * - Stores MIME type for proper file handling
     * - Includes file size for client-side optimization
     * - Removes storage prefixes to abstract storage implementation details
     */
    public static String storedMediaName(String mediaPrefix, MultipartFile document, String storedFileName) {
        // Create document metadata and GCS filename
        String cleanFileName = storedFileName.startsWith(mediaPrefix)
                ? storedFileName.substring(mediaPrefix.length())
                : storedFileName;  // get after the prefix to get the actual file name
        return String.format("MIME Type: %s, Size: %d KB, GCS File: %s",
                document.getContentType(),
                document.getSize() / 1024,
                cleanFileName);
    }

    /**
     * Validates document MIME types to ensure security and compatibility.
     * <p>
     * Why this exists:
     * - Prevents upload of potentially malicious files by restricting to known safe formats
     * - Ensures uploaded documents can be properly processed by the system
     * - Maintains consistency in supported document types across the application
     * - Makes document type validation centralized and reusable
     *
     * @param contentType The MIME type of the uploaded document
     * @return boolean indicating if the document type is supported
     */
    private boolean isValidDocumentType(String contentType) {
        Set<String> validTypes = Set.of(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
        return validTypes.contains(contentType.toLowerCase());
    }

    /**
     * Maps file extensions to their corresponding audio MIME types for proper content serving.
     * <p>
     * Why this exists:
     * - Ensures correct content-type headers when serving audio files
     * - Enables proper audio playback in web browsers
     * - Supports a variety of common audio formats for broader compatibility
     * - Helps clients determine how to handle the audio content
     *
     * @param fileExtension The file extension of the audio file
     * @return The corresponding MIME type or null if not supported
     */
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

    /**
     * Extracts file extension from filename for content type determination.
     * <p>
     * Why this exists:
     * - Enables dynamic content type detection without relying on client-provided MIME types
     * - Supports proper file handling and storage organization
     * - Helps validate file types before processing
     * - Used for generating appropriate content-type headers when serving files
     *
     * @param fileName The complete filename including extension
     * @return The file extension or null if no extension found
     */
    private String getFileExtension(String fileName) {
        if (fileName == null) return null;
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : null;
    }


    /**
     * Implements multi-level access control for content access.
     * <p>
     * Why this exists:
     * - Enforces the principle of the least privilege in content access
     * - Prevents unauthorized access to user data
     * - Implements role-based access control (admin vs regular users)
     * - Ensures users can only access their own WhatsApp content
     * - Handles various authentication edge cases (unauthenticated, unauthorized, unlinked accounts)
     * <p>
     * Access Rules:
     * 1. Admins have universal access
     * 2. Regular users can only access their own content
     * 3. Users must be authenticated and authorized
     * 4. WhatsApp numbers must be linked to accounts
     * 5. Supports both international and local number formats
     *
     * @param authentication The current authentication context
     * @param phoneNumber The WhatsApp number being accessed
     * @return AccessControlResponse with the access decision and any error messages
     */
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