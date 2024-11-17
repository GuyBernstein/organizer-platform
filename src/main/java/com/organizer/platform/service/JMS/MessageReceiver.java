package com.organizer.platform.service.JMS;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.organizer.platform.model.WhatsApp.WhatsAppMessage;
import com.organizer.platform.service.AI.AiService;
import com.organizer.platform.service.Google.CloudStorageService;
import com.organizer.platform.service.WhatsApp.WhatsAppMessageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class MessageReceiver {
    private final AiService aiService;
    private final WhatsAppMessageService messageService;
    private final ObjectMapper objectMapper;
    private final CloudStorageService cloudStorageService;

    @Autowired
    public MessageReceiver(AiService aiService, WhatsAppMessageService messageService,
                           ObjectMapper objectMapper, CloudStorageService cloudStorageService) {
        this.aiService = aiService;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.cloudStorageService = cloudStorageService;
    }

    @JmsListener(destination = "exampleQueue")
    public void processMessage(String serializedMessage) {
        try {
            // Deserialize the JSON string back to WhatsAppMessage object
            WhatsAppMessage whatsAppMessage = objectMapper.readValue(serializedMessage, WhatsAppMessage.class);

            log.info("Received message from queue: {}", whatsAppMessage.getMessageContent());

            // Validate message
            validateMessage(whatsAppMessage);
            whatsAppMessage.setProcessed(true);

            String messageContent = whatsAppMessage.getMessageContent();

            // Store category from AI response
            if(whatsAppMessage.getMessageType().equals("text"))
                whatsAppMessage.setCategory(aiService.generateCategoryFromText(messageContent));
            else if (whatsAppMessage.getMessageType().equals("image")) {
                // Extract image name from metadata
                String imageName = extractImageNameFromMetadata(messageContent);

                // Fetch and convert image to base64
                String base64Image = fetchAndConvertImageToBase64(imageName);
                whatsAppMessage.setCategory(aiService.generateCategoryFromImage(base64Image));
            }


            // Save to database
            whatsAppMessage = messageService.save(whatsAppMessage);
            log.info("Successfully saved message to database with ID: {}", whatsAppMessage.getId());
        } catch (JsonProcessingException e) {
            log.error("Error deserializing message from queue", e);
            throw new RuntimeException("Error processing message from queue", e);
        } catch (UnirestException e) {
            throw new RuntimeException("Error processing category from ai in queue", e);
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

    private String extractImageNameFromMetadata(String metadata) {
        Pattern pattern = Pattern.compile("GCS File: (.+)$");
        Matcher matcher = pattern.matcher(metadata);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        throw new RuntimeException("Could not extract image name from metadata");
    }

    private String fetchAndConvertImageToBase64(String imageName) {
        try {
            // Get pre-signed URL
            String preSignedUrl = cloudStorageService.generateSignedUrl(imageName);

            // Fetch image content
            URL url = new URL(preSignedUrl);
            try (InputStream inputStream = url.openStream()) {
                byte[] imageBytes = IOUtils.toByteArray(inputStream);
                return Base64.getEncoder().encodeToString(imageBytes);
            }
        } catch (Exception e) {
            log.error("Error fetching and converting image", e);
            throw new RuntimeException("Failed to process image: " + imageName, e);
        }
    }
}
