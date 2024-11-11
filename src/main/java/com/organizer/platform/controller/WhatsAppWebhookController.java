package com.organizer.platform.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.organizer.platform.model.Message;
import com.organizer.platform.model.WhatsAppMessage;
import com.organizer.platform.model.WhatsAppWebhookRequest;
import com.organizer.platform.repository.WhatsAppMessageRepository;
import com.organizer.platform.util.Dates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;

import static com.organizer.platform.model.WhatsAppMessage.WhatsAppMessageBuilder.aWhatsAppMessage;

@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public WhatsAppWebhookController(WhatsAppMessageRepository messageRepository, JmsTemplate jmsTemplate, ObjectMapper objectMapper) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
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

    private static WhatsAppMessage createWhatsAppMessage(Message message) {
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
                    // Store image metadata in messageContent
                    String imageMetadata = String.format("Image ID: %s, MIME Type: %s",
                            message.getImage().getId(),
                            message.getImage().getMimeType());
                    builder.messageContent(imageMetadata);

                    // You might want to construct a URL to access the image later
                    // This is just an example - adjust according to your needs
                    String mediaUrl = String.format("/media/images/%s", message.getImage().getId());
                    builder.mediaUrl(mediaUrl);
                }
                break;
            default:
                logger.warn("Unsupported message type: {}", message.getType());
        }

        return builder.build();
    }
}

