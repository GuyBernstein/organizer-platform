package com.organizer.platform.service.WhatsApp;

import com.organizer.platform.model.WhatsApp.Image;
import com.organizer.platform.model.WhatsApp.MediaResponse;
import com.organizer.platform.service.Google.CloudStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Service
public class WhatsAppImageService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppImageService.class);
    private final CloudStorageService cloudStorageService;
    private final RestTemplate restTemplate;

    @Autowired
    public WhatsAppImageService(CloudStorageService cloudStorageService, RestTemplate restTemplate) {
        this.cloudStorageService = cloudStorageService;
        this.restTemplate = restTemplate;
    }

    public String processAndUploadImage(String from, Image whatsAppImage, String whatsAppToken) {
        // Download image from WhatsApp servers using their Media API
        byte[] imageData = downloadImageFromWhatsApp(whatsAppImage.getId(), whatsAppToken);


        if (imageData == null || imageData.length == 0) {
            throw new RuntimeException("Failed to download image from WhatsApp");
        }

        // Upload to Google Cloud Storage
        return cloudStorageService.uploadImage(
                from,
                imageData,
                whatsAppImage.getMimeType(),
                whatsAppImage.getId() + "." + getExtensionFromMimeType(whatsAppImage.getMimeType())
        );
    }

    private byte[] downloadImageFromWhatsApp(String mediaId, String token)  {
        try {
            // First, get the image URL using the media ID
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

            // Now download the actual image using the URL from the response
            HttpHeaders downloadHeaders = new HttpHeaders();
            downloadHeaders.setBearerAuth(token);

            ResponseEntity<byte[]> imageResponse = restTemplate.exchange(
                    mediaResponse.getBody().getUrl(),
                    HttpMethod.GET,
                    new HttpEntity<>(downloadHeaders),
                    byte[].class
            );

            return imageResponse.getBody();

        } catch (Exception e) {
            logger.error("Error downloading image with ID: {}", mediaId, e);
            throw new RuntimeException("Failed to download image", e);
        }

    }


    private String getExtensionFromMimeType(String mimeType) {
        switch (mimeType.toLowerCase()) {
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            default:
                return "jpg";
        }
    }
}
