package com.organizer.platform.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.organizer.platform.model.WhatsApp.*;
import com.organizer.platform.model.organizedDTO.WhatsAppMessage;
import com.organizer.platform.service.User.UserService;
import com.organizer.platform.service.WhatsApp.WhatsAppAudioService;
import com.organizer.platform.service.WhatsApp.WhatsAppDocumentService;
import com.organizer.platform.service.WhatsApp.WhatsAppImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.organizer.platform.model.organizedDTO.WhatsAppMessage.WhatsAppMessageBuilder.aWhatsAppMessage;

/**
 * Controller responsible for handling WhatsApp webhook notifications and message processing.
 * <p>
 * This controller:
 * 1. Receives incoming webhook requests from WhatsApp's API
 * 2. Validates and processes different types of messages (text, image, document, audio)
 * 3. Uploads media content (images, documents, audio) to Google Cloud Storage
 * 4. Creates standardized WhatsAppMessage objects for further processing
 * 5. Sends processed messages to a JMS queue for asynchronous handling
 * 6. Ensures user authorization before processing messages
 * <p>
 * The controller supports multiple message types:
 * - Text messages: Processes plain text content
 * - Image messages: Uploads images to GCS and creates metadata
 * - Document messages: Uploads documents to GCS and creates metadata
 * - Audio messages: Uploads audio files to GCS and creates metadata
 * <p>
 * @RestController Handles REST endpoints for WhatsApp webhook
 * @RequestMapping("/webhook") Base path for webhook endpoints
 */
@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {
    // JMS template for sending messages to message queue
    private final JmsTemplate jmsTemplate;
    // Object mapper for JSON serialization/deserialization
    private final ObjectMapper objectMapper;
    // Service for processing and storing WhatsApp images
    private final WhatsAppImageService whatsAppImageService;
    // Service for processing and storing WhatsApp documents
    private final WhatsAppDocumentService whatsAppDocumentService;
    // Service for processing and storing WhatsApp audio files
    private final WhatsAppAudioService whatsAppAudioService;
    // Service for user management and authorization
    private final UserService userService;

    // WhatsApp API authentication token
    @Value("${whatsapp.api.token}")
    String whatsAppToken;

    @Autowired
    public WhatsAppWebhookController(JmsTemplate jmsTemplate, ObjectMapper objectMapper,
                                     WhatsAppImageService whatsAppImageService,
                                     WhatsAppDocumentService whatsAppDocumentService,
                                     WhatsAppAudioService whatsAppAudioService, UserService userService) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.whatsAppImageService = whatsAppImageService;
        this.whatsAppDocumentService = whatsAppDocumentService;
        this.whatsAppAudioService = whatsAppAudioService;
        this.userService = userService;
    }

    /**
     * Main webhook endpoint that receives incoming WhatsApp messages.
     * <p>
     * This method:
     * 1. Receives the webhook payload from WhatsApp
     * 2. Validates user authorization for each message
     * 3. Processes authorized messages through the message pipeline
     * 4. Handles any errors during processing
     *
     * @param webhookRequest The incoming webhook request containing WhatsApp message data
     * @return ResponseEntity with status 200 if successful, 500 if processing fails
     */
    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody WhatsAppWebhookRequest webhookRequest) {
        try {

            webhookRequest.getEntry().forEach(entry -> entry.getChanges().forEach(change -> {
                if (change.getValue().getMessages() != null && !change.getValue().getMessages().isEmpty()) {
                    Message message = change.getValue().getMessages().get(0); // get the message

                    try {
                        // Only process message if user is authorized
                        if (userService.processNewUser(message.getFrom())) {
                            processMessage(message);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }));

            return ResponseEntity.ok("Message processed");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing message");
        }
    }

    /**
     * Processes individual WhatsApp messages by converting them to a standardized format
     * and sending them to a message queue for asynchronous processing.
     * <p>
     * This method:
     * 1. Creates a standardized WhatsAppMessage object from the raw message
     * 2. Serializes the message to JSON format
     * 3. Sends the serialized message to a JMS queue for further processing
     *
     * @param message The raw WhatsApp message to process
     * @throws RuntimeException if message processing or serialization fails
     */
    private void processMessage(Message message) {
        try {
            WhatsAppMessage whatsAppMessage = createWhatsAppMessage(message);

            // Serialize the WhatsAppMessage to JSON string
            String serializedMessage = objectMapper.writeValueAsString(whatsAppMessage);

            // Send the serialized JSON string to the queue
            jmsTemplate.convertAndSend("exampleQueue", serializedMessage);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing message", e);
        }
    }

    /**
     * Creates a standardized WhatsAppMessage object from the incoming webhook message.
     * This method serves as a message type router, ensuring consistent message processing
     * regardless of the incoming message type (text, image, document, or audio).
     * <p>
     * The builder pattern is used here to:
     * 1. Maintain consistent message structure across all message types
     * 2. Allow for flexible message construction as WhatsApp adds new message types
     * 3. Ensure all required fields are populated before message creation
     *
     * @param message The raw incoming WhatsApp webhook message
     * @return A structured WhatsAppMessage ready for queue processing
     */
    private WhatsAppMessage createWhatsAppMessage(Message message) {
        // Create WhatsAppMessage entity
        WhatsAppMessage.WhatsAppMessageBuilder builder = aWhatsAppMessage()
                .fromNumber(message.getFrom())
                .messageType(message.getType())
                .processed(false);

        switch (message.getType().toLowerCase()) {
            case "text":
                processTextMessage(message, builder);
                break;
            case "image":
                processImageMessage(message, builder);
                break;
            case "document":
                processDocumentMessage(message, builder);
                break;
            case "audio":
                processAudioMessage(message, builder);
                break;
            default:
        }


        return builder.build();
    }

    /**
     * Processes audio messages by uploading them to Google Cloud Storage.
     * This separation of audio processing allows for:
     * - Specialized audio file handling and validation
     * - Future audio-specific features (like transcription or duration checks)
     * - Independent scaling of audio processing resources
     * <p>
     * The stored file location is then encoded in the message metadata for downstream processing.
     */
    private void processAudioMessage(Message message, WhatsAppMessage.WhatsAppMessageBuilder builder) {
        if (message.getAudio() != null) {
            // Upload audio to Google Cloud Storage
            String storedFileName = whatsAppAudioService.processAndUploadAudio(
                    message.getFrom(),
                    message.getAudio(),
                    whatsAppToken
            );

            String audioMetadata = createAudioMetadata(message.getAudio(), storedFileName);
            builder.messageContent(audioMetadata);
        }
    }


    /**
     * Handles document messages through Google Cloud Storage upload.
     * Separate document processing is crucial for:
     * - Supporting various document formats (PDF, DOC, etc.)
     * - Implementing document-specific security scanning
     * - Managing document retention policies
     * <p>
     * Document metadata is preserved to maintain file context and enable proper handling
     * by downstream processors.
     */
    private void processDocumentMessage(Message message, WhatsAppMessage.WhatsAppMessageBuilder builder) {
        if (message.getDocument() != null) {
            // Upload document to Google Cloud Storage
            String storedFileName = whatsAppDocumentService.processAndUploadDocument(
                    message.getFrom(),
                    message.getDocument(),
                    whatsAppToken
            );

            String documentMetadata = createDocumentMetadata(message.getDocument(), storedFileName);
            builder.messageContent(documentMetadata);
        }
    }

    /**
     * Manages image message processing and storage.
     * Dedicated image processing enables:
     * - Image format validation and optimization
     * - Potential future image analysis or manipulation
     * - Separate image storage optimization and CDN integration
     * <p>
     * Images are stored with metadata to maintain context and enable features like
     * galleries or image search.
     */
    private void processImageMessage(Message message, WhatsAppMessage.WhatsAppMessageBuilder builder) {
        if (message.getImage() != null) {
            // Upload image to Google Cloud Storage
            String storedFileName = whatsAppImageService.processAndUploadImage(
                    message.getFrom(),
                    message.getImage(),
                    whatsAppToken
            );

            String imageMetadata = createImageMetadata(message, storedFileName);
            builder.messageContent(imageMetadata);
        }
    }

    /**
     * Processes plain text messages.
     * While simpler than media processing, separate text handling allows for:
     * - Future text analysis or natural language processing
     * - Implementation of text-specific features (like command processing)
     * - Consistent message structure regardless of content type
     */
    private static void processTextMessage(Message message, WhatsAppMessage.WhatsAppMessageBuilder builder) {
        if (message.getText() != null) {
            builder.messageContent(message.getText().getBody());
        }
    }

    /**
     * Creates structured metadata for audio files.
     * This standardized format is important for:
     * - Maintaining consistent audio file references across the system
     * - Enabling efficient audio file lookup and management
     * - Supporting audio-specific features in downstream processing
     * <p>
     * The format strips storage-specific prefixes to maintain clean references
     * while preserving essential audio metadata.
     */
    private String createAudioMetadata(Audio audio, String storedFileName) {
        String cleanFileName = storedFileName.startsWith("audios/")
                ? storedFileName.substring("audios/".length())
                : storedFileName;  // get after the prefix "audios/" to get the actual file name
        return String.format("Audio ID: %s, MIME Type: %s, GCS File: %s",
                audio.getId(),
                audio.getMimeType(),
                cleanFileName);
    }


    /**
     * Creates standardized metadata for image files.
     * This structured format is essential for:
     * - Enabling consistent image tracking across different system components
     * - Supporting image galleries and search functionality
     * - Facilitating image lifecycle management and cleanup
     * <p>
     * The clean filename approach (removing storage prefixes) maintains platform
     * independence and simplifies integration with various frontend components
     * while preserving the original GCS reference.
     */
    public static String createImageMetadata(Message message, String storedFileName) {
        // Create image metadata and GCS filename
        String cleanFileName = storedFileName.startsWith("images/")
                ? storedFileName.substring("images/".length())
                : storedFileName;  // get after the prefix "images/" to get the actual file name
        return String.format("Image ID: %s, MIME Type: %s, GCS File: %s",
                message.getImage().getId(),
                message.getImage().getMimeType(),
                cleanFileName);
    }

    /**
     * Creates standardized metadata for document files.
     * This structured approach is crucial for:
     * - Maintaining document traceability throughout the system
     * - Supporting document search and categorization features
     * - Managing document versioning and access control
     * <p>
     * The metadata format aligns with other media types for consistency
     * while capturing document-specific attributes needed for proper
     * handling in downstream processing.
     */
    private String createDocumentMetadata(Document document, String storedFileName) {
        String cleanFileName = storedFileName.startsWith("documents/")
                ? storedFileName.substring("documents/".length())
                : storedFileName;  // get after the prefix "documetnts/" to get the actual file name
        return String.format("Document ID: %s, MIME Type: %s, GCS File: %s",
                document.getId(),
                document.getMimeType(),
                cleanFileName);
    }
}

