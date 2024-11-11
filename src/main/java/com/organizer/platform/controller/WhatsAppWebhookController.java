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

    private final WhatsAppMessageRepository messageRepository;
    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public WhatsAppWebhookController(WhatsAppMessageRepository messageRepository, JmsTemplate jmsTemplate, ObjectMapper objectMapper) {
        this.messageRepository = messageRepository;
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
    }


    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody WhatsAppWebhookRequest webhookRequest) {
        try {
            logger.info("Received webhook request: {}", webhookRequest);

            webhookRequest.getEntry().forEach(entry -> entry.getChanges().forEach(change -> {
                if (change.getValue().getMessages() != null && !change.getValue().getMessages().isEmpty()) {
                    Message message = change.getValue().getMessages().get(0);
                    if ("text".equals(message.getType())) {
                        try {
                            // Create WhatsAppMessage entity
                            WhatsAppMessage whatsAppMessage = aWhatsAppMessage()
                                    .fromNumber(message.getFrom())
                                    .messageType(message.getType())
                                    .messageContent(message.getText().getBody())
                                    .processed(false)
                                    .build();

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
                }
            }));

            return ResponseEntity.ok("Message processed");
        } catch (Exception e) {
            logger.error("Error processing webhook request", e);
            return ResponseEntity.status(500).body("Error processing message");
        }
    }
}

