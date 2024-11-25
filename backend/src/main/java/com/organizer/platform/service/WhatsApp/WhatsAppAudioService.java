package com.organizer.platform.service.WhatsApp;

import com.organizer.platform.model.WhatsApp.Audio;
import com.organizer.platform.model.WhatsApp.MediaResponse;
import com.organizer.platform.service.Google.CloudStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WhatsAppAudioService {
    private final CloudStorageService cloudStorageService;
    private final RestTemplate restTemplate;

    @Autowired
    public WhatsAppAudioService(CloudStorageService cloudStorageService, RestTemplate restTemplate) {
        this.cloudStorageService = cloudStorageService;
        this.restTemplate = restTemplate;
    }

    public String processAndUploadAudio(String from, Audio whatsAppAudio, String whatsAppToken) {
        // Download document from WhatsApp servers using their Media API
        byte[] audioData = downloadAudioFromWhatsApp(whatsAppAudio.getId(), whatsAppToken);

        if (audioData == null || audioData.length == 0) {
            throw new RuntimeException("Failed to download audio from WhatsApp");
        }

        // Upload to Google Cloud Storage
        return cloudStorageService.uploadAudio(
                from,
                audioData,
                whatsAppAudio.getMimeType(),
                whatsAppAudio.getId() + "." + getExtensionFromMimeType(whatsAppAudio.getMimeType())
        );
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
            return audioResponse.getBody();

        } catch (Exception e) {
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
                return "ogg";
        }
    }
}
