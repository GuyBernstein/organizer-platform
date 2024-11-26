package com.organizer.platform.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.organizer.platform.model.User.AppUser;
import com.organizer.platform.model.WhatsApp.*;
import com.organizer.platform.model.organizedDTO.WhatsAppMessage;
import com.organizer.platform.service.User.UserService;
import com.organizer.platform.service.WhatsApp.WhatsAppAudioService;
import com.organizer.platform.service.WhatsApp.WhatsAppDocumentService;
import com.organizer.platform.service.WhatsApp.WhatsAppImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.organizer.platform.model.organizedDTO.WhatsAppMessage.WhatsAppMessageBuilder.aWhatsAppMessage;

@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    private final WhatsAppImageService whatsAppImageService;
    private final WhatsAppDocumentService whatsAppDocumentService;
    private final WhatsAppAudioService whatsAppAudioService;
    private final UserService userService;


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

    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody WhatsAppWebhookRequest webhookRequest) {
        try {
            logger.info("Received webhook request: {}", webhookRequest);

            webhookRequest.getEntry().forEach(entry -> entry.getChanges().forEach(change -> {
                if (change.getValue().getMessages() != null && !change.getValue().getMessages().isEmpty()) {
                    Message message = change.getValue().getMessages().get(0); // get the message
                    String whatsappNumber = message.getFrom(); // and the number

                    try {
                        // Create unauthorized user if number is not authorized, or if the user does not exist
                        if (!userService.isAuthorizedNumber(message.getFrom())) {
                            userService.createUnauthorizedUser(whatsappNumber);
                        }
                        processMessage(message);
                    } catch (Exception e) {
                        logger.error("Failed to create unauthorized user for number: " + whatsappNumber, e);
                    }
                }
            }));

            return ResponseEntity.ok("Message processed");
        } catch (Exception e) {
            logger.error("Error processing webhook request", e);
            return ResponseEntity.status(500).body("Error processing message");
        }
    }

    private void processMessage(Message message) {
        try {
            WhatsAppMessage whatsAppMessage = createWhatsAppMessage(message);

            // Serialize the WhatsAppMessage to JSON string
            String serializedMessage = objectMapper.writeValueAsString(whatsAppMessage);
            logger.info("Serialized message sent to JMS queue: {}", serializedMessage);

            // Send the serialized JSON string to the queue
            jmsTemplate.convertAndSend("exampleQueue", serializedMessage);

        } catch (JsonProcessingException e) {
            logger.error("Error serializing WhatsAppMessage", e);
            throw new RuntimeException("Error processing message", e);
        }
    }

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
                logger.warn("Unsupported message type: {}", message.getType());
        }


        return builder.build();
    }

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
        } else {
            logger.warn("Audio message received but audio content is null");
        }
    }

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
        } else {
            logger.warn("Document message received but document content is null");
        }
    }

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
        } else {
            logger.warn("Image message received but image content is null");
        }
    }

    private static void processTextMessage(Message message, WhatsAppMessage.WhatsAppMessageBuilder builder) {
        if (message.getText() != null) {
            builder.messageContent(message.getText().getBody());
        } else {
            logger.warn("Text message received but text content is null");
        }
    }

    private String createAudioMetadata(Audio audio, String storedFileName) {
        String cleanFileName = storedFileName.startsWith("audios/")
                ? storedFileName.substring("audios/".length())
                : storedFileName;  // get after the prefix "audios/" to get the actual file name
        return String.format("Audio ID: %s, MIME Type: %s, GCS File: %s",
                audio.getId(),
                audio.getMimeType(),
                cleanFileName);
    }

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

