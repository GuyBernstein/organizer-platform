package com.organizer.platform.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.organizer.platform.model.WhatsAppMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageReceiver {

    private final WhatsAppMessageService messageService;
    private final ObjectMapper objectMapper;

    @Autowired
    public MessageReceiver(WhatsAppMessageService messageService, ObjectMapper objectMapper) {
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    @JmsListener(destination = "exampleQueue")
    public void processMessage(String serializedMessage) {
        try {
            // Deserialize the JSON string back to WhatsAppMessage object
            WhatsAppMessage whatsAppMessage = objectMapper.readValue(serializedMessage, WhatsAppMessage.class);

            log.info("Received message from queue: {}", whatsAppMessage.getMessageContent());

            // Validate message
            validateMessage(whatsAppMessage);

            // Set processed flag
            whatsAppMessage.setProcessed(true);

            // Save to database
            whatsAppMessage = messageService.save(whatsAppMessage);
            log.info("Successfully saved message to database with ID: {}", whatsAppMessage.getId());
        } catch (JsonProcessingException e) {
            log.error("Error deserializing message from queue", e);
            throw new RuntimeException("Error processing message from queue", e);
        }
    }


    private void validateMessage(WhatsAppMessage message) {
        if (message.getFromNumber() == null || message.getFromNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("FromNumber cannot be null or empty");
        }
        if (message.getMessageType() == null || message.getMessageType().trim().isEmpty()) {
            throw new IllegalArgumentException("MessageType cannot be null or empty");
        }
        // Ensure required fields are set
        if (message.getCreatedAt() == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null or empty");
        }
    }
}
