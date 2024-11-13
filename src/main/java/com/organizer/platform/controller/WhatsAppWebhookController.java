package com.organizer.platform.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.organizer.platform.model.Document;
import com.organizer.platform.model.Message;
import com.organizer.platform.model.WhatsAppMessage;
import com.organizer.platform.model.WhatsAppWebhookRequest;
import com.organizer.platform.service.WhatsAppDocumentService;
import com.organizer.platform.service.WhatsAppImageService;
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

import static com.organizer.platform.model.WhatsAppMessage.WhatsAppMessageBuilder.aWhatsAppMessage;

@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    private final WhatsAppImageService whatsAppImageService;
    private final WhatsAppDocumentService whatsAppDocumentService;

    @Value("${whatsapp.api.token}")
    String whatsAppToken;

    @Autowired
    public WhatsAppWebhookController(JmsTemplate jmsTemplate, ObjectMapper objectMapper,
                                     WhatsAppImageService whatsAppImageService,
                                     WhatsAppDocumentService whatsAppDocumentService) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.whatsAppImageService = whatsAppImageService;
        this.whatsAppDocumentService = whatsAppDocumentService;
    }


    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody WhatsAppWebhookRequest webhookRequest) {
        try {
            logger.info("Received webhook request: {}", webhookRequest);

            webhookRequest.getEntry().forEach(entry -> entry.getChanges().forEach(change -> {
                logger.info("Processing entry with ID: {}", entry.getId());

                if (change.getValue().getMessages() != null && !change.getValue().getMessages().isEmpty()) {
                    Message message = change.getValue().getMessages().get(0);
                    processMessage(message);
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

            // Send the serialized JSON string to the queue
            jmsTemplate.convertAndSend("exampleQueue", serializedMessage);

            logger.info("Serialized message sent to JMS queue: {}", serializedMessage);
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
            default:
                logger.warn("Unsupported message type: {}", message.getType());
        }

        return builder.build();
    }

    private void processDocumentMessage(Message message, WhatsAppMessage.WhatsAppMessageBuilder builder) {
        if (message.getDocument() != null) {
            // Upload document to Google Cloud Storage
            String storedFileName = whatsAppDocumentService.processAndUploadDocument(
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

    private static String createImageMetadata(Message message, String storedFileName) {
        // Create image metadata and GCS filename
        return String.format("Image ID: %s, MIME Type: %s, GCS File: %s",
                message.getImage().getId(),
                message.getImage().getMimeType(),
                storedFileName);
    }

    private String createDocumentMetadata(Document document, String storedFileName) {
        String cleanFileName = storedFileName.startsWith("documents/")
                ? storedFileName.substring("documents/".length())
                : storedFileName;  // get after the prefix "documetnts/" to get the actual file name
        return String.format("Document ID: %s, MIME Type: %s, Filename: %s",
                document.getId(),
                document.getMimeType(),
                cleanFileName);
    }
}

