package com.organizer.platform.service.WhatsApp;

import com.organizer.platform.model.WhatsApp.Document;
import com.organizer.platform.model.organizedDTO.MediaResponse;
import com.organizer.platform.service.Google.CloudStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service responsible for handling WhatsApp document messages.
 * This service downloads documents from WhatsApp's servers and uploads them to Google Cloud Storage.
 * Unlike other media types, it preserves the original filename when storing the document.
 */
@Service
public class WhatsAppDocumentService {
    private final CloudStorageService cloudStorageService;
    private final RestTemplate restTemplate;


    @Autowired
    public WhatsAppDocumentService(CloudStorageService cloudStorageService, RestTemplate restTemplate) {
        this.cloudStorageService = cloudStorageService;
        this.restTemplate = restTemplate;
    }

    /**
     * Processes a document message from WhatsApp and uploads it to Google Cloud Storage.
     * Maintains the original filename when storing in Cloud Storage.
     *
     * @param from The sender's WhatsApp number
     * @param whatsAppDocument The document message data from WhatsApp
     * @param whatsAppToken The authentication token for WhatsApp's API
     * @return The filename/path where the document was stored in Cloud Storage
     * @throws RuntimeException if download or upload fails
     */
    public String processAndUploadDocument(String from, Document whatsAppDocument, String whatsAppToken) {
        // Download document from WhatsApp servers using their Media API
        byte[] documentData = downloadDocumentFromWhatsApp(whatsAppDocument.getId(), whatsAppToken);

        if (documentData == null || documentData.length == 0) {
            throw new RuntimeException("Failed to download document from WhatsApp");
        }

        // Upload to Google Cloud Storage

        return cloudStorageService.uploadDocument(
                from,
                documentData,
                whatsAppDocument.getMimeType(),
                whatsAppDocument.getFilename()  // Use original filename
        );
    }

    /**
     * Downloads document from WhatsApp's servers using their Media API.
     * This is a two-step process:
     * 1. Get the media URL using the media ID
     * 2. Download the actual document from the retrieved URL
     *
     * @param mediaId The ID of the media to download
     * @param token WhatsApp API authentication token
     * @return The downloaded document as a byte array
     * @throws RuntimeException if the download fails
     */
    private byte[] downloadDocumentFromWhatsApp(String mediaId, String token)  {
        try {
            // First, get the document URL using the media ID
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

            // Now download the actual document using the URL from the response
            HttpHeaders downloadHeaders = new HttpHeaders();
            downloadHeaders.setBearerAuth(token);

            ResponseEntity<byte[]> documentResponse = restTemplate.exchange(
                    mediaResponse.getBody().getUrl(),
                    HttpMethod.GET,
                    new HttpEntity<>(downloadHeaders),
                    byte[].class
            );

            return documentResponse.getBody();

        } catch (Exception e) {
            throw new RuntimeException("Failed to download document", e);
        }

    }
}