package com.organizer.platform.service;

import com.organizer.platform.model.Audio;
import com.organizer.platform.model.MediaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WhatsAppAudioService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppDocumentService.class);
    private final CloudStorageService cloudStorageService;
    private final RestTemplate restTemplate;

    @Autowired
    public WhatsAppAudioService(CloudStorageService cloudStorageService, RestTemplate restTemplate) {
        this.cloudStorageService = cloudStorageService;
        this.restTemplate = restTemplate;
    }

    public String processAndUploadAudio(Audio whatsAppAudio, String whatsAppToken) {
        logger.info("Audio ID: {}, Filename: {}, MIME Type: {}",
                whatsAppAudio.getId(),
                whatsAppAudio.getVoice(),
                whatsAppAudio.getMimeType());
        logger.info("Downloading audio with ID: {} using token: {}", whatsAppAudio.getId(), whatsAppToken);

        // Download document from WhatsApp servers using their Media API
        byte[] audioData = downloadAudioFromWhatsApp(whatsAppAudio.getId(), whatsAppToken);

        if (audioData == null || audioData.length == 0) {
            throw new RuntimeException("Failed to download audio from WhatsApp");
        }

        // Upload to Google Cloud Storage
        String fileName = cloudStorageService.uploadAudio(
                audioData,
                whatsAppAudio.getMimeType(),
                whatsAppAudio.getId() + "." + getExtensionFromMimeType(whatsAppAudio.getMimeType())
        );

        logger.info("Successfully uploaded document to GCS with filename: {}", fileName);
        return fileName;
    }

    private byte[] downloadAudioFromWhatsApp(String mediaId, String token) {
        try {
            // First, get the audio URL using the media ID
            String mediaUrl = String.format("https://graph.facebook.com/v18.0/%s", mediaId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            ResponseEntity<MediaResponse> mediaResponse = restTemplate.exchange(
                    mediaUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    MediaResponse.class
            );

            if (mediaResponse.getBody() == null || mediaResponse.getBody().getUrl() == null) {
                throw new RuntimeException("Failed to get media URL");
            }

            // Now download the actual audio using the URL from the response
            HttpHeaders downloadHeaders = new HttpHeaders();
            downloadHeaders.setBearerAuth(token);

            ResponseEntity<byte[]> audioResponse = restTemplate.exchange(
                    mediaResponse.getBody().getUrl(),
                    HttpMethod.GET,
                    new HttpEntity<>(downloadHeaders),
                    byte[].class
            );

            logger.info("Successfully downloaded audio with ID: {}", mediaId);
            return audioResponse.getBody();

        } catch (Exception e) {
            logger.error("Error downloading audio with ID: {}", mediaId, e);
            throw new RuntimeException("Failed to download audio", e);
        }
    }

    private String getExtensionFromMimeType(String mimeType) {
        // Extract the main MIME type without parameters
        String baseMimeType = mimeType.split(";")[0].trim().toLowerCase();

        switch (baseMimeType) {
            case "audio/ogg":
                return "ogg";
            case "audio/mpeg":
                return "mp3";
            case "audio/mp4":
                return "m4a";
            case "audio/wav":
                return "wav";
            case "audio/webm":
                return "webm";
            case "audio/aac":
                return "aac";
            default:
                // Log unexpected MIME type
                logger.warn("Unexpected audio MIME type: {}. Defaulting to 'ogg'", mimeType);
                return "ogg";
        }
    }
}
