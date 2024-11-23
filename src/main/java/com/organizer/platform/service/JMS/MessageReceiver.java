package com.organizer.platform.service.JMS;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.exceptions.UnirestException;
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

import java.io.IOException;
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

            // Process message based on type
            processMessageByType(whatsAppMessage);
            log.info("Successfully saved message to database with ID: {}", whatsAppMessage.getId());
        } catch (JsonProcessingException e) {
            log.error("Error deserializing message from queue", e);
            throw new RuntimeException("Error processing message from queue", e);
        } catch (UnirestException e) {
            log.error("Error processing message content: {}", e.getMessage());
            throw new RuntimeException("Failed to process message content", e);
        }
    }

    private void processMessageByType(WhatsAppMessage whatsAppMessage) throws UnirestException, JsonProcessingException {
        switch (whatsAppMessage.getMessageType().toLowerCase()) {
            case "text":
                aiService.generateOrganizationFromText(whatsAppMessage);
                break;

            case "image":
                String imageName = extractNameFromMetadata(whatsAppMessage.getMessageContent(),
                        whatsAppMessage.getFromNumber() + "/");
                String base64Image = fetchAndConvertToBase64(whatsAppMessage.getFromNumber(), imageName, "image");
                aiService.generateOrganizationFromImage(base64Image, whatsAppMessage);
                break;

            case "document":
                String pdfName = extractNameFromMetadata(whatsAppMessage.getMessageContent(),
                        whatsAppMessage.getFromNumber() + "/");
                if(pdfName.endsWith(".pdf")){
                    String base64pdf = fetchAndConvertToBase64(whatsAppMessage.getFromNumber(), pdfName, "pdf");
                    aiService.generateOrganizationFromPDF(base64pdf, whatsAppMessage);
                }
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

    private String extractNameFromMetadata(String metadata, String from) {
        Pattern pattern = Pattern.compile("GCS File: " + Pattern.quote(from) + "(.+)$");
        Matcher matcher = pattern.matcher(metadata);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        throw new RuntimeException("Could not extract image name from metadata");
    }

    private String fetchAndConvertToBase64(String from, String fileName, String fileType) {
        if (fileName == null || fileType == null) {
            throw new IllegalArgumentException("fileName and fileType cannot be null");
        }

        try {
            // Get pre-signed URL
            String preSignedUrl = fileType.equals("image")
                    ? cloudStorageService.generateImageSignedUrl(from, fileName)
                    : cloudStorageService.generateDocumentSignedUrl(from, fileName);

            return fetchAndProcessContent(preSignedUrl, fileType);
        } catch (Exception e) {
            log.error("Error fetching and converting {}: {}", fileType, fileName, e);
            throw new RuntimeException(String.format("Failed to process %s: %s", fileType, fileName), e);
        }
    }

    private String fetchAndProcessContent(String preSignedUrl, String fileType)
            throws IOException {
        try (InputStream inputStream = new URL(preSignedUrl).openStream()) {
            byte[] fileBytes = IOUtils.toByteArray(inputStream);
            return processContent(fileBytes, fileType);
        }
    }

    private String processContent(byte[] fileBytes, String fileType) throws IOException {
        // returns a base64 of the media file
        if (fileType.equals("image")) {
            // Also resizing the image for better interaction with the AI
            ImageProcessor imageProcessor = new ImageProcessor();
            return imageProcessor.processAndConvertImageFromBytes(fileBytes);
        }
        return Base64.getEncoder().encodeToString(fileBytes);
    }
}
