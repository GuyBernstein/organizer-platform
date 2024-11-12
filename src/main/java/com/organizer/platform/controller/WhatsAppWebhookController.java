package com.organizer.platform.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.organizer.platform.model.Message;
import com.organizer.platform.model.WhatsAppMessage;
import com.organizer.platform.model.WhatsAppWebhookRequest;
import com.organizer.platform.service.CloudStorageService;
import com.organizer.platform.service.WhatsAppImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.organizer.platform.model.WhatsAppMessage.WhatsAppMessageBuilder.aWhatsAppMessage;

@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    private final WhatsAppImageService whatsAppImageService;

    @Value("${whatsapp.api.token}")
    String whatsAppToken;

    @Autowired
    public WhatsAppWebhookController(JmsTemplate jmsTemplate, ObjectMapper objectMapper,
                                     WhatsAppImageService whatsAppImageService) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.whatsAppImageService = whatsAppImageService;
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
                if (message.getText() != null) {
                    builder.messageContent(message.getText().getBody());
                }
                break;
            case "image":
                if (message.getImage() != null) {
                    // Upload image to Google Cloud Storage
                    String storedFileName = whatsAppImageService.processAndUploadImage(
                            message.getImage(),
                            whatsAppToken
                    );

                    // Store image metadata and GCS filename
                    String imageMetadata = String.format("Image ID: %s, MIME Type: %s, GCS File: %s",
                            message.getImage().getId(),
                            message.getImage().getMimeType(),
                            storedFileName);
                    builder.messageContent(imageMetadata);
                    builder.mediaUrl(storedFileName);
                }
                break;
            default:
                logger.warn("Unsupported message type: {}", message.getType());
        }

        return builder.build();
    }
}

