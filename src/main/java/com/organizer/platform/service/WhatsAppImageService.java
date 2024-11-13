package com.organizer.platform.service;

import com.organizer.platform.model.Image;
import com.organizer.platform.model.MediaResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    public String processAndUploadImage(Image whatsAppImage, String whatsAppToken) {
        logger.info("Image ID: {} Image sha256 {} Image mymetype {}", whatsAppImage.getId(), whatsAppImage.getSha256(),
        whatsAppImage.getMimeType());
        logger.info("Downloading image with ID: {} using token: {}", whatsAppImage.getId(), whatsAppToken);

        // Download image from WhatsApp servers using their Media API
        byte[] imageData = downloadImageFromWhatsApp(whatsAppImage.getId(), whatsAppToken);


        if (imageData == null || imageData.length == 0) {
            throw new RuntimeException("Failed to download image from WhatsApp");
        }

        // Upload to Google Cloud Storage
        String fileName = cloudStorageService.uploadImage(
                imageData,
                whatsAppImage.getMimeType(),
                whatsAppImage.getId() + "." + getExtensionFromMimeType(whatsAppImage.getMimeType())
        );

        logger.info("Successfully uploaded image to GCS with filename: {}", fileName);
        return fileName;
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

            System.out.println("Objects.requireNonNull(mediaResponse.getBody()).getUrl(): " + Objects.requireNonNull(mediaResponse.getBody()).getUrl());

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

            logger.info("Successfully downloaded image with ID: {}", mediaId);
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
