package com.organizer.platform.service.WhatsApp;

import com.organizer.platform.model.WhatsApp.Image;
import com.organizer.platform.model.organizedDTO.MediaResponse;
import com.organizer.platform.service.Google.CloudStorageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Service responsible for handling WhatsApp image messages.
 * This service downloads images from WhatsApp's servers and uploads them to Google Cloud Storage.
 * It includes additional error handling and validation compared to other media services.
 */
@Service
public class WhatsAppImageService {
    private final CloudStorageService cloudStorageService;
    private final RestTemplate restTemplate;

    @Autowired
    public WhatsAppImageService(CloudStorageService cloudStorageService, RestTemplate restTemplate) {
        this.cloudStorageService = cloudStorageService;
        this.restTemplate = restTemplate;
    }

    /**
     * Processes an image message from WhatsApp and uploads it to Google Cloud Storage.
     *
     * @param from The sender's WhatsApp number
     * @param whatsAppImage The image message data from WhatsApp
     * @param whatsAppToken The authentication token for WhatsApp's API
     * @return The filename/path where the image was stored in Cloud Storage
     * @throws RuntimeException if download or upload fails
     */
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

    /**
     * Downloads image from WhatsApp's servers using their Media API.
     * Includes comprehensive error handling and validation of inputs.
     *
     * @param mediaId The ID of the media to download
     * @param token WhatsApp API authentication token
     * @return The downloaded image as a byte array
     * @throws IllegalArgumentException if token or mediaId is empty
     * @throws RuntimeException for various download failures
     */
    private byte[] downloadImageFromWhatsApp(String mediaId, String token) {
        if (StringUtils.isEmpty(token)) {
            throw new IllegalArgumentException("WhatsApp API token cannot be empty");
        }

        try {
            // Validate the media ID
            if (StringUtils.isEmpty(mediaId)) {
                throw new IllegalArgumentException("Media ID cannot be empty");
            }

            // First, get the image URL using the media ID
            String mediaUrl = String.format("https://graph.facebook.com/v18.0/%s", mediaId);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token.trim()); // Trim the token to remove any whitespace
            headers.setContentType(MediaType.APPLICATION_JSON);

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
            downloadHeaders.setBearerAuth(token.trim());

            ResponseEntity<byte[]> imageResponse = restTemplate.exchange(
                    mediaResponse.getBody().getUrl(),
                    HttpMethod.GET,
                    new HttpEntity<>(downloadHeaders),
                    byte[].class
            );

            if (imageResponse.getBody() == null) {
                throw new RuntimeException("Downloaded image data is empty");
            }

            return imageResponse.getBody();

        } catch (HttpClientErrorException.Unauthorized e) {
            throw new RuntimeException("Invalid or expired WhatsApp API token", e);
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Failed to download image: " + e.getStatusText(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download image", e);
        }
    }

    /**
     * Determines the appropriate file extension based on the MIME type.
     *
     * @param mimeType The MIME type of the image file
     * @return The corresponding file extension (defaults to jpg)
     */
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
