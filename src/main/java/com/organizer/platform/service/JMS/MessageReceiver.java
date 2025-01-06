package com.organizer.platform.service.JMS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.organizer.platform.model.organizedDTO.WhatsAppMessage;
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

/**
 * Component responsible for processing WhatsApp messages received through JMS queue.
 * This service handles different types of messages (text, image, document, audio)
 * and coordinates with various services for AI processing, storage, and message management.
 */
@Slf4j
@Component
public class MessageReceiver {
    private final AiService aiService;
    private final WhatsAppMessageService messageService;
    private final ObjectMapper objectMapper;
    private final CloudStorageService cloudStorageService;

    /**
     * Constructor initializing required services for message processing.
     *
     * @param aiService AI service for message content analysis
     * @param messageService Service for WhatsApp message persistence
     * @param objectMapper JSON serialization/deserialization utility
     * @param cloudStorageService Service for cloud storage operations
     */
    @Autowired
    public MessageReceiver(AiService aiService, WhatsAppMessageService messageService,
                           ObjectMapper objectMapper, CloudStorageService cloudStorageService) {
        this.aiService = aiService;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.cloudStorageService = cloudStorageService;
    }

    /**
     * Main message processing method triggered by JMS messages from 'exampleQueue'.
     * Handles the complete lifecycle of message processing including:
     * - Message deserialization
     * - Validation
     * - Media processing
     * - AI analysis
     * - Message persistence
     *
     * @param serializedMessage JSON string containing WhatsApp message data
     */
    @JmsListener(destination = "exampleQueue")
    public void processMessage(String serializedMessage) {
        try {
            WhatsAppMessage whatsAppMessage = objectMapper.readValue(serializedMessage, WhatsAppMessage.class);
            validateMessage(whatsAppMessage);

            String mediaName = "";
            if(!whatsAppMessage.getMessageType().equals("text"))
                mediaName = extractNameFromMetadata(whatsAppMessage.getMessageContent(),
                        whatsAppMessage.getFromNumber() + "/");

            validateAIProcessing(whatsAppMessage, mediaName);
            saveAfterProcessedMessage(whatsAppMessage, mediaName);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing message from queue", e);
        } catch (UnirestException e) {
            throw new RuntimeException("Failed to process message content", e);
        }
    }

    /**
     * Determines whether a message requires AI processing based on its type.
     * Sets appropriate flags and categories for different message types.
     *
     * @param whatsAppMessage The message to be validated
     * @param mediaName Name of the media file if present
     */
    private static void validateAIProcessing(WhatsAppMessage whatsAppMessage, String mediaName) {
        switch (whatsAppMessage.getMessageType().toLowerCase()) {
            case "text":
            case "image":
                whatsAppMessage.setProcessed(true);
                break;
            case "document":
                whatsAppMessage.setProcessed(mediaName.toLowerCase().endsWith(".pdf"));
                if (!whatsAppMessage.isProcessed()) {
                    whatsAppMessage.setCategory("קבצים אחרים");
                    whatsAppMessage.setSubCategory(mediaName);
                }
                break;
            case "audio":
                whatsAppMessage.setCategory("קבצים אחרים");
                whatsAppMessage.setSubCategory(mediaName);
                break;
        }
    }

    /**
     * Handles message persistence and processing workflow.
     * Saves the message initially if new, processes it based on type,
     * and saves again if AI processing was performed.
     *
     * @param whatsAppMessage Message to be saved and processed
     * @param mediaName Name of associated media file
     * @throws UnirestException If message processing fails
     * @throws JsonProcessingException If JSON processing fails
     */
    private void saveAfterProcessedMessage(WhatsAppMessage whatsAppMessage, String mediaName)
            throws UnirestException, JsonProcessingException {
        if (whatsAppMessage.getId() == null) {
            messageService.save(whatsAppMessage);
        }

        processMessageByType(whatsAppMessage, mediaName);

        if (whatsAppMessage.isProcessed()) {
            messageService.save(whatsAppMessage);
        }
    }

    /**
     * Processes messages based on their type (text, image, document, audio).
     * Coordinates with AI service for content analysis and organization.
     *
     * @param whatsAppMessage Message to be processed
     * @param mediaName Name of associated media file
     * @throws UnirestException If message processing fails
     * @throws JsonProcessingException If JSON processing fails
     */
    private void processMessageByType(WhatsAppMessage whatsAppMessage, String mediaName)
            throws UnirestException, JsonProcessingException {
        switch (whatsAppMessage.getMessageType().toLowerCase()) {
            case "text":
                String purpose = whatsAppMessage.getPurpose();
                if (purpose != null && !purpose.isBlank()) {
                    aiService.generateOrganizationFromURL(whatsAppMessage);
                    break;
                }
                aiService.generateOrganizationFromText(whatsAppMessage);
                break;

            case "image":
                String base64Image = fetchAndConvertToBase64(whatsAppMessage.getFromNumber(), mediaName, "image");
                aiService.generateOrganizationFromImage(base64Image, whatsAppMessage);
                break;

            case "document":
                if(mediaName.toLowerCase().endsWith(".pdf")){
                    String base64pdf = fetchAndConvertToBase64(whatsAppMessage.getFromNumber(), mediaName, "pdf");
                    aiService.generateOrganizationFromPDF(base64pdf, whatsAppMessage);
                }
                break;

            case "audio":
                break;

            default:
                throw new IllegalArgumentException("Unsupported message type: " + whatsAppMessage.getMessageType());
        }
    }

    /**
     * Validates required fields in the WhatsApp message.
     * Ensures all necessary fields are present and properly formatted.
     *
     * @param message Message to be validated
     * @throws IllegalArgumentException If validation fails
     */
    private void validateMessage(WhatsAppMessage message) {
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

    /**
     * Extracts filename from message metadata using regex pattern matching.
     *
     * @param metadata Message metadata containing file information
     * @param from Sender's identifier
     * @return Extracted filename
     * @throws RuntimeException If filename cannot be extracted
     */
    private String extractNameFromMetadata(String metadata, String from) {
        Pattern pattern = Pattern.compile("GCS File: " + Pattern.quote(from) + "(.+)$");
        Matcher matcher = pattern.matcher(metadata);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        throw new RuntimeException("Could not extract image name from metadata");
    }

    /**
     * Fetches media content and converts it to base64 format.
     * Handles both image and document file types.
     *
     * @param from Sender's identifier
     * @param fileName Name of the file to fetch
     * @param fileType Type of file (image/pdf)
     * @return Base64 encoded string of the file content
     * @throws IllegalArgumentException If required parameters are null
     * @throws RuntimeException If processing fails
     */
    private String fetchAndConvertToBase64(String from, String fileName, String fileType) {
        if (fileName == null || fileType == null) {
            throw new IllegalArgumentException("fileName and fileType cannot be null");
        }

        try {
            String preSignedUrl = fileType.equals("image")
                    ? cloudStorageService.generateImageSignedUrl(from, fileName)
                    : cloudStorageService.generateDocumentSignedUrl(from, fileName);

            return fetchAndProcessContent(preSignedUrl, fileType);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to process %s: %s", fileType, fileName), e);
        }
    }

    /**
     * Fetches content from a pre-signed URL and processes it.
     *
     * @param preSignedUrl URL to fetch content from
     * @param fileType Type of file being processed
     * @return Processed content as base64 string
     * @throws IOException If content fetching or processing fails
     */
    private String fetchAndProcessContent(String preSignedUrl, String fileType)
            throws IOException {
        try (InputStream inputStream = new URL(preSignedUrl).openStream()) {
            byte[] fileBytes = IOUtils.toByteArray(inputStream);
            return processContent(fileBytes, fileType);
        }
    }

    /**
     * Processes file content based on type.
     * For images, includes additional processing for AI optimization.
     *
     * @param fileBytes Raw file content
     * @param fileType Type of file being processed
     * @return Processed content as base64 string
     * @throws IOException If processing fails
     */
    private String processContent(byte[] fileBytes, String fileType) throws IOException {
        if (fileType.equals("image")) {
            MediaProcessor mediaProcessor = new MediaProcessor();
            return mediaProcessor.processAndConvertImageFromBytes(fileBytes);
        }
        return Base64.getEncoder().encodeToString(fileBytes);
    }
}