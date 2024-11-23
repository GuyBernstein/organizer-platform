package com.organizer.platform.service.WhatsApp;

import com.organizer.platform.model.WhatsApp.Document;
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
public class WhatsAppDocumentService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppDocumentService.class);
    private final CloudStorageService cloudStorageService;
    private final RestTemplate restTemplate;


    @Autowired
    public WhatsAppDocumentService(CloudStorageService cloudStorageService, RestTemplate restTemplate) {
        this.cloudStorageService = cloudStorageService;
        this.restTemplate = restTemplate;
    }

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
            logger.error("Error downloading document with ID: {}", mediaId, e);
            throw new RuntimeException("Failed to download document", e);
        }

    }
}