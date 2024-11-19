package com.organizer.platform.service.JMS;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.organizer.platform.model.organizedDTO.Tag;
import com.organizer.platform.model.organizedDTO.WhatsAppMessage;
import com.organizer.platform.repository.TagRepository;
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MessageReceiver {
    private final AiService aiService;
    private final WhatsAppMessageService messageService;
    private final ObjectMapper objectMapper;
    private final CloudStorageService cloudStorageService;
    private final TagRepository tagRepository;

    @Autowired
    public MessageReceiver(AiService aiService, WhatsAppMessageService messageService,
                           ObjectMapper objectMapper, CloudStorageService cloudStorageService, TagRepository tagRepository) {
        this.aiService = aiService;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.cloudStorageService = cloudStorageService;
        this.tagRepository = tagRepository;
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

            // firstly, save the message to process it in the database.
            whatsAppMessage = messageService.save(whatsAppMessage);

            // Get just the names only if there are tags
            String tagNames = tagRepository.findAll().stream()
                    .map(Tag::getName)
                    .collect(Collectors.collectingAndThen(
                            Collectors.toSet(),
                            names -> String.join(", ", names)
                    ));

            // Process message based on type
            processMessageByType(whatsAppMessage, tagNames);
            log.info("Successfully saved message to database with ID: {}", whatsAppMessage.getId());
        } catch (JsonProcessingException e) {
            log.error("Error deserializing message from queue", e);
            throw new RuntimeException("Error processing message from queue", e);
        } catch (UnirestException e) {
            log.error("Error processing message content: {}", e.getMessage());
            throw new RuntimeException("Failed to process message content", e);
        }
    }

    private void processMessageByType(WhatsAppMessage whatsAppMessage, String tagNames) throws UnirestException, JsonProcessingException {
        switch (whatsAppMessage.getMessageType().toLowerCase()) {
            case "text":
                if(tagNames.isEmpty()) {
                    aiService.generateOrganizationFromText(whatsAppMessage, " ");

                } else {
                    aiService.generateOrganizationFromText(whatsAppMessage, tagNames);
                }
                break;

            case "image":
                String imageName = extractImageNameFromMetadata(whatsAppMessage.getMessageContent());
                String base64Image = fetchAndConvertImageToBase64(imageName);
                whatsAppMessage.setCategory(aiService.generateCategoryFromImage(base64Image));
                break;

            default:
                throw new IllegalArgumentException("Unsupported message type: " + whatsAppMessage.getMessageType());
        }
    }

    private void validateMessage(WhatsAppMessage message) {
        // Ensure message has required fields for persistence
        if (message.getFromNumber() == null || message.getFromNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("FromNumber cannot be null or empty");
        }
        if (message.getMessageType() == null || message.getMessageType().trim().isEmpty()) {
            throw new IllegalArgumentException("MessageType cannot be null or empty");
        }
        if (message.getCreatedAt() == null) {
            throw new IllegalArgumentException("CreatedAt cannot be null");
        }
        if (message.getMessageContent() == null) {
            throw new IllegalArgumentException("MessageContent cannot be null");
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
