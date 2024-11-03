package com.enhanceai.platform.whatsapp;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@CrossOrigin // Enable CORS
public class WhatsAppWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody WhatsAppWebhookRequest webhookRequest) {
        try {
            logger.info("Received webhook request: {}", webhookRequest);

            webhookRequest.getEntry().forEach(entry -> {
                entry.getChanges().forEach(change -> {
                    if (change.getValue().getMessages() != null && !change.getValue().getMessages().isEmpty()) {
                        Message message = change.getValue().getMessages().get(0);
                        if ("text".equals(message.getType())) {
                            String phoneNumber = message.getFrom();
                            String messageText = message.getText().getBody();
                            logger.info("Message from {}: {}", phoneNumber, messageText);
                            // Your business logic here
                        }
                    }
                });
            });

            return ResponseEntity.ok("Message processed");
        } catch (Exception e) {
            logger.error("Error processing webhook request", e);
            return ResponseEntity.status(500).body("Error processing message");
        }
    }
}

