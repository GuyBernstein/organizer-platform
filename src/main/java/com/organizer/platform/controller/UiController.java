package com.organizer.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.organizer.platform.model.ScraperDTO.ProcessingResult;
import com.organizer.platform.model.User.AppUser;
import com.organizer.platform.model.User.UserActivityDTO;
import com.organizer.platform.model.User.UserRole;
import com.organizer.platform.model.organizedDTO.MessageDTO;
import com.organizer.platform.model.organizedDTO.MessageTypeCount;
import com.organizer.platform.model.organizedDTO.WhatsAppMessage;
import com.organizer.platform.service.Google.CloudStorageService;
import com.organizer.platform.service.Scraper.ContentProcessorService;
import com.organizer.platform.service.Scraper.WebContentScraperService;
import com.organizer.platform.service.User.ExportService;
import com.organizer.platform.service.User.UserService;
import com.organizer.platform.service.WhatsApp.WhatsAppMessageService;
import com.organizer.platform.util.Dates;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.organizer.platform.controller.AppController.storedMediaName;
import static com.organizer.platform.model.User.AppUser.UserBuilder.anUser;
import static com.organizer.platform.model.organizedDTO.WhatsAppMessage.WhatsAppMessageBuilder.aWhatsAppMessage;

/**
 * Controller handling UI operations for the WhatsApp message organization platform.
 * This controller manages user authentication, message handling, and administrative functions.
 * It provides endpoints for viewing, creating, updating, and managing WhatsApp messages,
 * as well as user management and authentication flows.
 */
@Controller
@RequestMapping({"/"})
public class UiController {
    // Core service dependencies
    private final UserService userService;
    private final WhatsAppMessageService messageService;
    private final WebContentScraperService scraperService;
    private final ObjectMapper objectMapper;
    private final JmsTemplate jmsTemplate;
    private final CloudStorageService cloudStorageService;
    private final ExportService exportService;

    @Autowired
    public UiController(UserService userService, WhatsAppMessageService messageService,
                        WebContentScraperService scraperService, ObjectMapper objectMapper,
                        JmsTemplate jmsTemplate, CloudStorageService cloudStorageService, ExportService exportService) {
        this.userService = userService;
        this.messageService = messageService;
        this.scraperService = scraperService;
        this.objectMapper = objectMapper;
        this.jmsTemplate = jmsTemplate;
        this.cloudStorageService = cloudStorageService;
        this.exportService = exportService;
    }


    /*
      Main page endpoints and authentication handlers
     */

    /**
     * Handles the application's root endpoint, serving as the entry point for all users.
     * This method provides differentiated access based on authentication status to ensure
     * proper security and user experience - anonymous users see a public home page while
     * authenticated users get personalized content. This separation is crucial for
     * maintaining security boundaries while still providing public access to basic site info.
     *
     * @param principal The authenticated user info, null for anonymous users
     * @param model     The Spring MVC model for view rendering
     * @return The appropriate view name based on authentication status
     */
    @GetMapping
    public String home(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/home");
        }
        return handleAuthorizedAccess(principal, model, "דף הבית", "pages/home", false);
    }

    /**
     * Global exception handler for the UI controller that provides a consistent error
     * handling strategy across all endpoints. This centralized approach ensures:
     * - Users always get meaningful feedback when errors occur
     * - The application maintains a consistent look and feel even during errors
     * - Error messages are properly localized (in Hebrew)
     * - Authentication state is preserved during error handling
     * <p>
     * This is particularly important for maintaining a good user experience even
     * when things go wrong.
     *
     * @param model     For passing error details to the view
     * @param principal Current user's authentication info
     * @param ex        The caught exception
     * @return The error view name
     */
    @ExceptionHandler(Exception.class)
    public String handleAllExceptions(Model model,
                                      @AuthenticationPrincipal OAuth2User principal,
                                      Exception ex) {
        // Create a detailed error message
        String errorMessage = "\nאופס! משהו השתבש\n" + ex.getMessage();

        // Add the error information to the model
        model.addAttribute("errorMessage", errorMessage);

        if (principal == null) {
            model.addAttribute("content", "pages/auth/login");
        }
        model.addAttribute("content", "pages/home");
        return "layout/base";
    }


    /**
     * Provides access to the user's dashboard, which serves as the main control center
     * for authenticated users. This endpoint is authentication-gated to ensure that
     * sensitive user data and controls are only available to logged-in users, redirecting
     * anonymous users to the login page for security. The dashboard is critical as it's
     * the primary interface for users to manage their platform experience.
     *
     * @param principal The authenticated user's information
     * @param model     The Spring MVC model for view rendering
     * @return The appropriate view based on authentication status
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }
        return handleAuthorizedAccess(principal, model, "לוח בקרה", "pages/index", false);
    }

    /**
     * Handles access to the message management interface, enforcing authentication
     * to protect user message privacy. This endpoint is crucial for the platform's
     * core functionality as it provides users with their primary workspace for
     * viewing and managing their WhatsApp messages. Authentication checking here
     * ensures that users can only access their own messages.
     *
     * @param principal The authenticated user's information
     * @param model     The Spring MVC model for view rendering
     * @return The messages view or login redirect as appropriate
     */
    @GetMapping("/messages")
    public String messages(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }
        return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages", false);
    }

    /**
     * Provides access to administrative functions, with strict authentication controls.
     * This endpoint is particularly sensitive as it exposes platform management capabilities,
     * requiring both authentication and proper authorization (admin role) for access.
     * The admin interface is essential for platform governance, allowing privileged users
     * to manage user accounts, monitor system usage, and maintain platform health.
     *
     * @param principal The authenticated user's information
     * @param model     The Spring MVC model for view rendering
     * @return The admin panel view or appropriate redirect
     */
    @GetMapping("/admin")
    public String admin(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }
        return handleAuthorizedAccess(principal, model, "ניהול", "pages/admin", false);
    }

    /**
     * Manages the OAuth2 login flow and post-login user provisioning. This method serves
     * multiple critical purposes:
     * - Handles initial login attempts
     * - Processes logout confirmations
     * - Ensures new users are properly registered in the system
     * - Manages the transition from authentication to authorized access
     * <p>
     * The method is essential for maintaining security while providing a smooth
     * onboarding experience, automatically creating user records for new OAuth2 users
     * while ensuring existing users are properly recognized.
     *
     * @param principal The authenticated user's information
     * @param logout    Optional logout parameter
     * @param model     The Spring MVC model for view rendering
     * @return Appropriate view based on authentication state and user status
     */
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
        return handleAuthorizedAccess(principal, model, "לוח בקרה", "pages/index", false);
    }



    /*
      Message Management endpoints
     */

    /**
     * Handles message deletion requests with proper authorization checks.
     * <p>
     * This endpoint exists to allow users to remove outdated or irrelevant messages
     * from their history while maintaining data integrity. The deletion is permanent
     * to comply with data minimization principles and user privacy requests.
     * <p>
     * Authorization is enforced at two levels:
     * 1. Through Spring Security's @AuthenticationPrincipal
     * 2. Through the messageService's internal checks
     * <p>
     * Success/failure feedback is provided to users via redirect attributes
     * to maintain a consistent user experience across page refreshes.
     */
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

        return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages", false);
    }

    /**
     * Enables manual message updates with fine-grained control over all message attributes.
     * <p>
     * This endpoint supports detailed message curation by allowing users to:
     * - Recategorize messages for better organization
     * - Update message metadata (tags, next steps) for improved searchability
     * - Correct or enhance message content while preserving the original context
     * <p>
     * The manual update approach is chosen here to:
     * 1. Give users direct control over message organization
     * 2. Allow for human judgment in categorization
     * 3. Support bulk updates of related fields
     * <p>
     * Tags and next steps are handled as comma-separated values to support:
     * - Multiple value input through a simple text interface
     * - Easy parsing and validation
     * - Flexible tag management without complex UI requirements
     */
    @PostMapping("/messages/update")
    public String editTextMessage(
            @RequestParam Long messageId,
            @RequestParam String type,
            @RequestParam String purpose,
            @RequestParam String messageContent,
            @RequestParam String tags,
            @RequestParam String nextSteps,
            @RequestParam String category,
            @RequestParam String subCategory,
            @AuthenticationPrincipal OAuth2User principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }

        try {
            var message = messageService.findMessageById(messageId);
            if (message.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "אירעה שגיאה בעדכון ההודעה");
                return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages", false);
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
                    .category(category)
                    .subCategory(subCategory)
                    .tags(commaSeparatedTags)
                    .nextSteps(commaSeparatedNextSteps)
                    .build();

            messageService.partialUpdateMessage(message.get(), messageDTO);

            redirectAttributes.addFlashAttribute("successMessage", "ההודעה עודכנה");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "אירעה שגיאה בעדכון ההודעה");
        }

        return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages", false);
    }

    /**
     * Provides intelligent message updating with automated content analysis and categorization.
     * <p>
     * This endpoint exists to automate message organization by:
     * 1. Analyzing message content for embedded URLs and rich content
     * 2. Processing content through ML/AI for smart categorization
     * 3. Queuing updates for asynchronous processing to handle high loads
     * <p>
     * The "smart" update process involves:
     * 1. Content extraction and URL scraping for enhanced context
     * 2. Clearing existing relationships to prevent stale categorizations
     * 3. Async processing via JMS queue to handle long-running operations
     * <p>
     * This approach is particularly useful for:
     * - Bulk message reorganization
     * - Processing messages with complex content (URLs, media)
     * - Maintaining system responsiveness under load
     * <p>
     * Note: Messages are processed asynchronously to prevent UI blocking
     * and enable scalable processing of large message volumes.
     */
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
            if (message.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "אירעה שגיאה בעדכון ההודעה");
                return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages", false);
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

        return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages", false);
    }

    /**
     * Handles creation of text-based messages with URL content processing capabilities.
     * <p>
     * This endpoint serves as the entry point for manual message creation, primarily used for:
     * - Testing purposes during development and debugging
     * - Recovery scenarios where messages need to be manually re-added
     * - Supporting direct user input through the platform interface
     * <p>
     * The method implements asynchronous processing by:
     * 1. Detecting and processing any URLs in the message content
     * 2. Queuing, using JMS, the message for AI-driven categorization and analysis
     * This async approach prevents user interface blocking during potentially
     * time-consuming web scraping and analysis operations.
     */
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

        return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages", false);
    }

    /**
     * Manages the upload and storage of media files (images and documents) as messages.
     * <p>
     * This endpoint is critical for:
     * - Maintaining a complete archive of all communication types
     * - Supporting rich media content analysis
     * - Ensuring proper file organization by user/phone number
     * <p>
     * The method employs a multi-stage approach:
     * 1. Content type detection to properly categorize and store different media types
     * 2. Secure cloud storage with user-specific paths for data isolation
     * 3. Asynchronous processing queue integration for metadata extraction and analysis
     * <p>
     * This architecture enables scalable media handling while maintaining
     * consistent message processing workflows across all content types.
     */
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
            String messageType;

            String contentType = file.getContentType();
            if (contentType != null && contentType.startsWith("image/")) {
                // Handle image file
                storedFileName = cloudStorageService.uploadImage(phoneNumber,
                        file.getBytes(),
                        file.getContentType(),
                        file.getOriginalFilename()
                );
                metadata = storedMediaName("images/", file, storedFileName);
                messageType = "image";
            } else {
                // Handle document file
                storedFileName = cloudStorageService.uploadDocument(phoneNumber,
                        file.getBytes(),
                        file.getContentType(),
                        file.getOriginalFilename()
                );
                metadata = storedMediaName("documents/", file, storedFileName);
                messageType = "document";
            }


            // message creation
            WhatsAppMessage message = aWhatsAppMessage()
                    .fromNumber(phoneNumber)
                    .messageContent(metadata)
                    .messageType(messageType)
                    .processed(false)
                    .build();


            // Serialize and send to queue
            String serializedMessage = objectMapper.writeValueAsString(message);
            jmsTemplate.convertAndSend("exampleQueue", serializedMessage);

            redirectAttributes.addFlashAttribute("successMessage", "ההודעה נשלחה לעיבוד בהצלחה");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "אירעה שגיאה ביצירת ההודעה");
        }

        return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages", false);
    }

    /**
     * Provides filtered message views based on user-selected tags.
     * <p>
     * This endpoint is essential for:
     * - Enabling efficient message retrieval in large datasets
     * - Supporting contextual message organization
     * - Facilitating task and topic-based workflow management
     * <p>
     * The filtering system uses tag-based navigation because:
     * - Tags provide flexible, user-defined categorization
     * - Multiple tags enable multidimensional classification
     * - Tag filtering supports both broad and narrow content discovery
     * <p>
     * The method maintains consistent view rendering whether filtered or unfiltered,
     * ensuring a seamless user experience during navigation.
     */
    @GetMapping("/messages/filter")
    public String filterMessagesByTags(@RequestParam(name = "selectedTags", required = false) Set<String> tagNames,
                                       @RequestParam String phoneNumber,
                                       @AuthenticationPrincipal OAuth2User principal,
                                       Model model) {
        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }
        // If no tags selected, selectedTags will be null
        if (tagNames == null || tagNames.isEmpty()) {
            return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages", false);
        }

        // reset the model attributes for filtration
        model.addAttribute("categories", messageService.getFilteredMessages(tagNames, phoneNumber));
        return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages", true);
    }

    /**
     * Provides full-text search capabilities across messages for a specific phone number.
     * This endpoint exists to help users quickly find relevant information without having
     * to navigate through category hierarchies or tag filters. Rather than using database-level
     * search, it filters pre-organized messages in memory to maintain consistency with the
     * existing category/subcategory structure.
     * <p>
     * The search is performed across the already-organized message structure to ensure that
     * results maintain their hierarchical context, making it easier for users to understand
     * where found messages fit within their overall organizational system.
     * <p>
     *
     * @param content     Search text to find in messages
     * @param phoneNumber User's phone number to scope the search
     * @param principal   OAuth user information for authentication
     * @param model       Spring MVC model for view rendering
     * @return View name for rendering search results
     */
    @GetMapping("/messages/search")
    public String searchMessages(@RequestParam String content,
                                 @RequestParam String phoneNumber,
                                 @AuthenticationPrincipal OAuth2User principal,
                                 Model model) {
        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }
        // If no tags selected, selectedTags will be null
        if (content == null || content.isEmpty()) {
            return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages", false);
        }

        // get the messages organized by that phone number
        Map<String, Map<String, List<MessageDTO>>> organizedMessages =
                messageService.findMessageContentsByFromNumberGroupedByCategoryAndGroupedBySubCategory(phoneNumber);

        // reset the model attributes for filtration
        model.addAttribute("categories", messageService.getSearchedMessages(content, organizedMessages));
        return handleAuthorizedAccess(principal, model, "הודעות", "pages/messages", true);
    }

    /**
     * Provides secure access to media files stored in cloud storage.
     * Rather than exposing direct cloud storage URLs, this endpoint acts as a security
     * gateway that:
     * 1. Verifies user authentication before allowing access
     * 2. Generates short-lived signed URLs for media access
     * 3. Maintains abstraction over the actual storage location
     * <p>
     * The redirect approach is used instead of direct streaming to:
     * - Reduce server load by letting the cloud provider handle the actual file transfer
     * - Enable CDN caching and optimization
     * - Allow for future changes in storage backend without affecting clients
     * <p>
     *
     * @param type      Media type (document/image/audio) to determine storage bucket
     * @param content   Metadata containing file reference
     * @param phone     User phone number for scoping access
     * @param principal OAuth user information for authentication
     * @return Redirect to signed cloud storage URL
     */
    @GetMapping("/messages/getMedia")
    public RedirectView getMediaMessages(@RequestParam String type,
                                         @RequestParam String content,
                                         @RequestParam String phone,
                                         @AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return new RedirectView("/login");
        }
        String mediaName = extractNameFromMetadata(content, phone + "/");
        switch (type) {
            case "document":
                return new RedirectView(cloudStorageService.generateDocumentSignedUrl(phone, mediaName));
            case "image":
                return new RedirectView(cloudStorageService.generateImageSignedUrl(phone, mediaName));
            case "audio":
                return new RedirectView(cloudStorageService.generateAudioSignedUrl(phone, mediaName));
            default:
                return new RedirectView("/login");

        }
    }

    /**
     * Enables data portability by allowing users to export their messages in Excel format.
     * This functionality is crucial for:
     * - Backup purposes
     * - Offline analysis
     * - Integration with other tools
     * - Compliance with data portability regulations
     * <p>
     * The export is streamed as bytes rather than saved to disk to:
     * - Reduce server storage requirements
     * - Minimize security risks from stored files
     * - Ensure users always get fresh data
     * - Handle concurrent exports efficiently
     * <p>
     *
     * @param phoneNumber User's phone number to scope the export
     * @return ResponseEntity containing Excel file as byte array
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String phoneNumber) {
        List<MessageDTO> messagesToExport = messageService.findMessagesFromNumber(phoneNumber)
                .stream()
                .map(messageService::convertToMessageDTO)
                .collect(Collectors.toList());

        byte[] fileContent = exportService.generateExportFile(messagesToExport);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "messages_export.xlsx");

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(fileContent);
    }

    /*
      User Management endpoints
     */

    /**
     * Revokes user access to the platform while preserving their account data.
     * This is preferable to deletion when:
     * - Temporarily suspending user access
     * - Investigating suspicious activity
     * - Handling expired subscriptions/permissions
     * - Maintaining audit trails while preventing new actions
     */
    @PostMapping("/admin/deauthorize-user")
    public String deauthorize(@RequestParam Long userId,
                              @AuthenticationPrincipal OAuth2User principal,
                              Model model) {
        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }
        userService.deauthorize(userId);
        return handleAuthorizedAccess(principal, model, "ניהול", "pages/admin", false);
    }

    /**
     * Restores user access to the platform after verification or suspension period.
     * Essential for:
     * - Completing user verification process
     * - Restoring access after temporary suspension
     * - Re-enabling accounts after payment/compliance issues resolved
     * - Implementing graduated access control
     */
    @PostMapping("/admin/authorize-user")
    public String authorize(@RequestParam Long userId,
                            @AuthenticationPrincipal OAuth2User principal,
                            Model model) {
        if (principal == null) {
            return setupAnonymousPage(model, "דף הבית", "pages/auth/login");
        }
        userService.authorize(userId);
        return handleAuthorizedAccess(principal, model, "ניהול", "pages/admin", false);
    }

    /**
     * Permanently removes a user and their associated data from the system.
     * Critical for:
     * - Complying with data protection regulations (GDPR right to be forgotten)
     * - Removing spam/fake accounts
     * - Handling account termination requests
     * - Maintaining system hygiene by removing obsolete accounts
     * Note: Consider soft deletion or deauthorization for temporary measures
     */
    @PostMapping("/admin/delete-user")
    public String deleteUser(@RequestParam Long userId,
                             @AuthenticationPrincipal OAuth2User principal,
                             Model model) {
        if (principal == null) {
            return setupAnonymousPage(model, "התחברות", "pages/auth/login");
        }
        userService.deleteById(userId);
        return handleAuthorizedAccess(principal, model, "ניהול", "pages/admin", false);
    }

    /**
     * Updates user's system privileges and access levels.
     * Essential for role-based access control (RBAC):
     * - Promoting users to admin roles
     * - Adjusting access levels based on user responsibility
     * - Managing dynamic permission changes
     * - Implementing principle of the least privilege
     * Security Note: This operation should be logged for audit purposes
     */
    @PostMapping("/admin/change-role")
    public String changeRole(@RequestParam Long userId,
                             @RequestParam UserRole newRole,
                             @AuthenticationPrincipal OAuth2User principal,
                             Model model) {
        if (principal == null) {
            return setupAnonymousPage(model, "התחברות", "pages/auth/login");
        }
        userService.changeRole(userId, newRole);
        return handleAuthorizedAccess(principal, model, "ניהול", "pages/admin", false);
    }

    /**
     * Provisions new user accounts in the system with specified parameters.
     * Key for controlled user onboarding:
     * - Supporting admin-driven account creation
     * - Implementing organizational user management
     * - Ensuring proper initial role assignment
     * - Facilitating bulk user provisioning
     * Security Note: Ensure proper validation of email and phone formats
     */
    @PostMapping("/admin/create-user")
    public String createUser(@RequestParam String email,
                             @RequestParam String phone,
                             @RequestParam UserRole role,
                             @AuthenticationPrincipal OAuth2User principal,
                             Model model) {
        if (principal == null) {
            return setupAnonymousPage(model, "התחברות", "pages/auth/login");
        }
        userService.createUserFromEmail(email, role, phone);
        return handleAuthorizedAccess(principal, model, "ניהול", "pages/admin", false);
    }

    /**
     * Handles WhatsApp number submission and validation for user authorization.
     * Supports both Israeli and international phone number formats.
     *
     * @param whatsappNumber The phone number submitted by the user
     * @param principal OAuth2User containing user authentication details
     * @param redirectAttributes For passing error messages between redirects
     * @param model Spring MVC Model for view attributes
     * @return The appropriate view name based on validation results
     */
    @PostMapping("/submit-whatsapp")
    public String submitWhatsApp(@RequestParam String whatsappNumber,
                                 @AuthenticationPrincipal OAuth2User principal,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        // Check if user is authenticated, redirect to login if not
        if (principal == null) {
            return setupAnonymousPage(model, "התחברות", "pages/auth/login");
        }

        // Validate for empty or null phone number input
        // Returns to login page with error message if validation fails
        if (whatsappNumber == null || whatsappNumber.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("phoneError", "אנא הזן מספר טלפון");
            return "redirect:/login";
        }

        // Define regex patterns for phone number validation
        // Israeli format: 10 digits starting with '05' (e.g., 0501234567)
        // International format: 12 digits starting with '972' (e.g., 972501234567)
        String israeliPattern = "^05\\d{8}$";  // 05XXXXXXXX
        String internationalPattern = "^972\\d{9}$";  // 972XXXXXXXXX

        // Check if the number matches either Israeli or international format
        boolean isValidNumber = whatsappNumber.matches(israeliPattern) || whatsappNumber.matches(internationalPattern);

        // If number doesn't match either format, return to login page with error message
        if (!isValidNumber) {
            redirectAttributes.addFlashAttribute("phoneError", "מספר הטלפון אינו תקין. אנא הזן מספר בפורמט 05XXXXXXXX או 972XXXXXXXXX");
            return "redirect:/login";
        }

        // Convert Israeli format to international format if necessary
        // Removes the leading '0' and adds '972' prefix
        if (whatsappNumber.matches("^05\\d{8}$")) {
            whatsappNumber = "972" + whatsappNumber.substring(1); // Remove '0' and add '972'
        }

        // Get user's email from OAuth2 principal and authorize the user
        String email = principal.getAttribute("email");
        userService.authorizeUserFromPhone(email, whatsappNumber);

        // Redirect to dashboard after successful authorization
        return handleAuthorizedAccess(principal, model, "לוח הבקרה", "pages/index", false);
    }

    /**
     * Extracts the actual filename from a metadata string for media files stored in cloud storage.
     * <p>
     * This method is necessary because media files (images, documents, etc.) are stored with
     * additional metadata information for tracking and management purposes. When we need to
     * retrieve these files, we need to parse out just the filename portion.
     * <p>
     * The metadata string includes information like MIME type, file size, and the actual GCS path.
     * For security and organization, files are stored in user-specific prefixes (phone numbers)
     * to maintain data isolation between users.
     * <p>
     *
     * @param metadata The full metadata string containing file information
     * @param prefix   The user-specific prefix (typically phone number) used for file organization
     * @return The extracted filename, or null if the pattern isn't found
     */
    private String extractNameFromMetadata(String metadata, String prefix) {
        // Example input: "MIME Type: image/png, Size: 2614 KB, GCS File: 972509603888/35fd8df1-78c4-40d1-ab48-1fba8648d82c.png"
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        Pattern pattern = Pattern.compile(prefix + "(.+)");
        Matcher matcher = pattern.matcher(metadata);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Central authorization and access control method for the application.
     * <p>
     * This method serves as a gateway for all authorized access attempts, implementing
     * a consistent authorization workflow across different pages. It's designed to:
     * 1. Verify user existence and authorization status
     * 2. Set up appropriate view models based on authorization level
     * 3. Maintain security by preventing unauthorized access to protected pages
     * <p>
     * The method supports filtered views (e.g., for search results) while maintaining
     * the same security and authorization checks, ensuring consistent user experience
     * regardless of how the page is accessed.
     * <p>
     *
     * @param principal   The OAuth2 user principal from the current session
     * @param model       The Spring MVC model for view rendering
     * @param title       The page title to display
     * @param contentPage The target content page to render
     * @param isFiltered  Whether this is a filtered view (affects data loading)
     * @return The name of the template to render
     */
    private String handleAuthorizedAccess(OAuth2User principal, Model model, String title, String contentPage, boolean isFiltered) {
        // Find or create user
        AppUser appUser = userService.ensureUserExists(principal);

        if (appUser.isAuthorized() && appUser.getRole() != UserRole.UNAUTHORIZED) {
            setupAuthorizedModel(model, appUser, title, contentPage, isFiltered);
        }
        else {
            setupUnauthorizedModel(model, principal, appUser);
        }
        return "layout/base";
    }

    /**
     * Prepares the view model for unauthenticated visitors to public pages.
     * <p>
     * This method serves as the default entry point for all public-facing pages,
     * implementing a consistent, minimalist view for users who haven't yet
     * authenticated. It's designed to:
     * 1. Provide a clean, unburdened interface for first-time visitors
     * 2. Act as a security boundary, ensuring unauthenticated users only see
     * appropriate content
     * 3. Maintain a consistent look and feel across all public pages while
     * stripping out any personalized or protected elements
     * <p>
     * The method deliberately avoids loading user-specific data or sensitive
     * information, making it both secure and performant for public access.
     *
     * @param model       The Spring MVC model
     * @param title       The page title to display
     * @param contentPage The public page content to render
     * @return The name of the template to render
     */
    private String setupAnonymousPage(Model model, String title, String contentPage) {
        model.addAttribute("title", title + " - Organizer Platform");
        model.addAttribute("content", contentPage);
        model.addAttribute("errorMessage", "");
        return "layout/base";
    }

    /**
     * Prepares the view model for unauthorized users or users awaiting authorization.
     * <p>
     * This method handles the special case of users who have authenticated but aren't
     * yet authorized to use the system. It's crucial for:
     * 1. Providing feedback to users about their authorization status
     * 2. Maintaining security by restricting access to authorized features
     * 3. Creating a welcoming experience for new users while they wait for approval
     * <p>
     * The method sets up a limited view with just enough information to let users know
     * their status without exposing any protected functionality.
     *
     * @param model     The Spring MVC model
     * @param principal The OAuth2 user information
     * @param appUser   The application user entity (may be unauthorized)
     */
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
        model.addAttribute("isAuthorized", appUser.isAuthorized());
        model.addAttribute("isRoleUnauthorized", appUser.getRole() == UserRole.UNAUTHORIZED);
    }

    /**
     * Configures the view model for authorized users based on their access context.
     * <p>
     * This method is the central point for preparing page-specific data and permissions.
     * It's designed to:
     * 1. Support different page types (messages, index, admin) with specific data needs
     * 2. Maintain consistent UI elements across all authorized pages
     * 3. Handle filtered views without duplicating setup logic
     * <p>
     * The method uses a strategy pattern through the switch statement to delegate
     * page-specific setup to specialized methods, keeping the code organized and
     * maintainable as new pages are added.
     *
     * @param model       The Spring MVC model
     * @param appUser     The authorized application user
     * @param title       The page title
     * @param contentPage The specific page being requested
     * @param isFiltered  Whether this is a filtered view
     */
    private void setupAuthorizedModel(Model model, AppUser appUser,
                                      String title, String contentPage, boolean isFiltered) {
        setupCommonAttributes(model, appUser, title, contentPage);

        switch (contentPage) {
            case "pages/messages":
                setupMessagesPage(model, appUser, isFiltered);
                break;
            case "pages/index":
                setupIndexPage(model, appUser);
                break;
            case "pages/admin":
                setupAdminPage(model);
                break;
            default:
                break;
        }
    }

    /**
     * Prepares the admin dashboard view with comprehensive user analytics and metrics.
     * This method aggregates user registration trends, message activity patterns, and
     * user role distributions to enable administrators to:
     * - Track platform growth through user registration timeline
     * - Monitor user engagement via message activity
     * - Maintain oversight of user authorization status and role distribution
     * <p>
     * The data is organized chronologically to show growth patterns and identify
     * usage trends over time.
     * <p>
     *
     * @param model Spring MVC Model to populate admin-dashboard data
     */
    private void setupAdminPage(Model model) {
        List<AppUser> users = userService.findAll();

        // Track daily user registration patterns to identify peak signup periods
        // and measure the effectiveness of marketing/outreach efforts
        Map<LocalDateTime, Long> userCountsByDate = users.stream()
                .map(user -> Dates.atLocalTime(user.getCreatedAt()))  // Convert to LocalDate
                .collect(Collectors.groupingBy(
                        date -> date,
                        TreeMap::new,  // TreeMap ensures chronological ordering for trend analysis
                        Collectors.counting()
                ));

        // Calculate cumulative growth to visualize platform adoption rate
        // and identify periods of accelerated or decelerated growth
        Map<LocalDateTime, Long> cumulativeCountsByDate = new TreeMap<>();
        long runningTotal = 0;
        for (Map.Entry<LocalDateTime, Long> entry : userCountsByDate.entrySet()) {
            runningTotal += entry.getValue();
            cumulativeCountsByDate.put(entry.getKey(), runningTotal);
        }

        // Generate per-user activity metrics to identify power users
        // and spot engagement patterns across the user base
        Map<Long, UserActivityDTO> activityDataMap = users.stream()
                .map(user -> {
                    List<WhatsAppMessage> messages = messageService.findMessagesFromNumber(user.getWhatsappNumber());
                    return UserActivityDTO.builder()
                            .userId(user.getId())
                            .username(user.getName())
                            // Aggregate messages by month to smooth out daily variations
                            // and provide a clearer view of sustained engagement
                            .messageCountByDate(messages.stream()
                                    .collect(Collectors.groupingBy(
                                            msg -> Dates.atLocalTime(msg.getCreatedAt())
                                                    .withDayOfMonth(1)
                                                    .withHourOfDay(0)
                                                    .withMinuteOfHour(0)
                                                    .withSecondOfMinute(0)
                                                    .withMillisOfSecond(0),
                                            Collectors.counting()
                                    )))
                            .build();
                })
                .collect(Collectors.toMap(
                        UserActivityDTO::getUserId,
                        activity -> activity
                ));

        // Create monthly message volume breakdown to identify seasonal patterns
        // and correlate message activity with business cycles
        Map<Long, Map<String, Long>> userMonthlyMessageCounts = users.stream()
                .collect(Collectors.toMap(
                        AppUser::getId,
                        user -> messageService.findMessagesFromNumber(user.getWhatsappNumber())
                                .stream()
                                .collect(Collectors.groupingBy(
                                        msg -> {
                                            DateTimeFormatter formatter = DateTimeFormat.forPattern("MM/yyyy");
                                            return Dates.atLocalTime(msg.getCreatedAt())
                                                    .withDayOfMonth(1)
                                                    .withHourOfDay(0)
                                                    .withMinuteOfHour(0)
                                                    .withSecondOfMinute(0)
                                                    .withMillisOfSecond(0)
                                                    .toString(formatter);
                                        },
                                        Collectors.counting()
                                ))
                ));


        // Make all analytics data available to the view for rendering
        model.addAttribute("users", users);
        model.addAttribute("userCountsByDate", userCountsByDate);
        model.addAttribute("cumulativeCountsByDate", cumulativeCountsByDate);
        model.addAttribute("messageCountsByMonth", userMonthlyMessageCounts);

        // Track user role distribution to monitor platform access patterns
        // and ensure proper authorization management
        model.addAttribute("authorizedUsers", users.stream().filter(AppUser::isNormalUser).count());
        model.addAttribute("unauthorizedUsers", users.stream().filter(AppUser::isUNAUTHORIZEDUser).count());
        model.addAttribute("adminUsers", users.stream().filter(AppUser::isAdmin).count());

    }

    /**
     * Prepares the dashboard view with analytics and metrics for user engagement.
     * This method aggregates various metrics to give users a comprehensive overview
     * of their message organization system, including:
     * - Tag usage to show content categorization effectiveness
     * - Message volume to indicate system utilization
     * - Category/subcategory counts to show organizational structure depth
     * - Next steps count to track actionable items
     * - Message type distribution to understand content patterns
     * - Category hierarchy to visualize content organization
     * <p>
     * This data helps users understand how they're using the platform and identify
     * areas that might need better organization or attention.
     */
    private void setupIndexPage(Model model, AppUser appUser) {
        // Fetch all tags to help users understand their content classification coverage
        model.addAttribute("totalTags", messageService.getAllTagsByPhoneNumber(appUser.getWhatsappNumber()));

        // Cache messages to avoid multiple database calls in subsequent operations
        List<WhatsAppMessage> messages = messageService.findMessagesFromNumber(appUser.getWhatsappNumber());
        model.addAttribute("totalMessages", messages.size());

        // Count unique categories while handling potential null values
        // This shows how well the user is utilizing the primary classification system
        long totalCategories = messages
                .stream()
                .map(WhatsAppMessage::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        model.addAttribute("categoriesCount", totalCategories);

        // Track subcategory usage to measure classification depth
        // High subcategory count might indicate need for category reorganization
        long totalSubCategories = messages
                .stream()
                .map(WhatsAppMessage::getSubCategory)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        model.addAttribute("subCategoriesCount", totalSubCategories);

        // Sum all next steps to gauge pending actions
        // This helps users track their follow-up items across all messages
        long totalNextSteps = messages
                .stream()
                .map(WhatsAppMessage::getNextSteps)
                .filter(Objects::nonNull)
                .mapToLong(Set::size)
                .sum();
        model.addAttribute("nextStepsCount", totalNextSteps);

        // Get message type distribution to understand content patterns
        // This helps users see what kinds of content they're managing most
        List<MessageTypeCount> messageTypes = messageService.getMessageTypesByPhoneNumber(appUser.getWhatsappNumber());
        model.addAttribute("messageTypes", messageTypes);

        // Build category hierarchy with fallback for uncategorized items
        // This creates a tree structure showing how content is distributed
        // across categories and subcategories
        Map<String, Map<String, Long>> hierarchy = messages.stream()
                .collect(Collectors.groupingBy(
                        msg -> msg.getCategory() != null ? msg.getCategory() : "UNCATEGORIZED",
                        Collectors.groupingBy(
                                msg -> msg.getSubCategory() != null ? msg.getSubCategory() : "UNCATEGORIZED",
                                Collectors.counting()
                        )
                ));
        model.addAttribute("categoriesHierarchy", hierarchy);
    }

    /**
     * Sets up essential user interface elements shared across different views.
     * This method ensures consistent presentation of user-specific information and
     * access control indicators throughout the application by:
     * - Maintaining consistent page titling conventions
     * - Displaying user identification and profile elements
     * - Exposing appropriate authorization flags for UI permission control
     * <p>
     * This centralized approach helps maintain UI consistency and simplifies
     * permission-based feature toggling in the views.
     *
     * @param model       Spring MVC Model to populate with common attributes
     * @param appUser     Current authenticated user
     * @param title       Page-specific title
     * @param contentPage Template path for the main content
     */
    private void setupCommonAttributes(Model model, AppUser appUser,
                                       String title, String contentPage) {
        model.addAttribute("title", title + " - Organizer Platform");
        model.addAttribute("name", appUser.getName());
        model.addAttribute("email", appUser.getEmail());
        model.addAttribute("picture", appUser.getPictureUrl());
        model.addAttribute("content", contentPage);
        model.addAttribute("phone", appUser.getWhatsappNumber());
        model.addAttribute("isAuthorized", appUser.isAuthorized());
        model.addAttribute("isRoleUnauthorized", appUser.getRole() == UserRole.UNAUTHORIZED);
        model.addAttribute("isAdmin", appUser.isAdmin());
    }

    /**
     * Prepares the message management view with organized content and filtering capabilities.
     * This method serves two key purposes:
     * 1. For regular views (isFiltered=false):
     * - Organizes messages hierarchically by category and subcategory to maintain
     * a clean, structured view of all content
     * - Extracts available categories and subcategories to power the UI's
     * filtering and organization features
     * <p>
     * 2. For filtered views (isFiltered=true):
     * - Preserves the filtered results while still providing access to the full
     * set of categories and tags for further filtering
     * <p>
     * The method ensures users can both browse their complete message hierarchy
     * and apply complex filters while maintaining context of their full content
     * organization structure.
     * <p>
     *
     * @param isFiltered Indicates whether the current view is showing filtered results
     *                   to prevent overwriting filtered content with unfiltered data
     */
    private void setupMessagesPage(Model model, AppUser appUser, boolean isFiltered) {
        // Get the categories list for dropdowns without storing the full messages
        List<String> categories = messageService.getAllCategories(appUser.getWhatsappNumber());
        List<String> subcategories = messageService.getAllSubcategories(appUser.getWhatsappNumber());

        if (!isFiltered) {
            Map<String, Map<String, List<MessageDTO>>> organizedMessages =
                    messageService.findMessageContentsByFromNumberGroupedByCategoryAndGroupedBySubCategory(appUser.getWhatsappNumber());
            model.addAttribute("categories", organizedMessages); // resets the filter option
        }

        model.addAttribute("categoryList", categories);
        model.addAttribute("subcategoryList", subcategories);
        model.addAttribute("totalTags", messageService.getAllTagsByPhoneNumber(appUser.getWhatsappNumber()));
    }

}
