package com.organizer.platform.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.organizer.platform.config.GCSProperties;
import com.organizer.platform.controller.WhatsAppWebhookController;
import com.organizer.platform.util.Dates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CloudStorageService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final GCSProperties gcsProperties;
    private Storage storage;
    private Bucket bucket;

    @Autowired
    public CloudStorageService(GCSProperties gcsProperties) {
        this.gcsProperties = gcsProperties;
    }

    @PostConstruct
    public void initialize() throws IOException {
        logger.info("Initializing Cloud Storage Service...");
        logger.info("Using project ID: {}", gcsProperties.getProjectId());
        logger.info("Using bucket name: {}", gcsProperties.getBucketName());

        try {
            Resource resource = new ClassPathResource(gcsProperties.getCredentialsPath().replace("classpath:", ""));
            GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream());
            logger.info("Successfully loaded credentials");

            storage = StorageOptions.newBuilder()
                    .setProjectId(gcsProperties.getProjectId())
                    .setCredentials(credentials)
                    .build()
                    .getService();
            logger.info("Successfully created Storage service");

            // Test bucket access
            bucket = storage.get(gcsProperties.getBucketName());
            if (bucket == null) {
                throw new IllegalStateException("Bucket not found: " + gcsProperties.getBucketName());
            }
            logger.info("Successfully connected to bucket: {}", gcsProperties.getBucketName());

            // Test bucket listing to verify permissions
            bucket.list().iterateAll().forEach(blob ->
                    logger.info("Found blob: {}", blob.getName())
            );

        } catch (Exception e) {
            logger.error("Failed to initialize Cloud Storage", e);
            throw e;
        }
    }

    public String uploadImage(byte[] imageData, String mimeType, String originalFileName) {
        // Generates a unique filename
        String fileName = generateFileName(originalFileName);

        // Creates a blob (file) in GCS
        BlobId blobId = BlobId.of(gcsProperties.getBucketName(), fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(mimeType)
                .build();

        // Uploads the file
        storage.create(blobInfo, imageData);
        return fileName;
    }

    public String generateSignedUrl(String imageName) {
        return generateSignedUrlForObject("images/" + imageName);
    }

    public String generateDocumentSignedUrl(String documentName) {
        return generateSignedUrlForObject("documents/" + documentName);
    }

    private String generateSignedUrlForObject(String objectPath) {
        BlobId blobId = BlobId.of(gcsProperties.getBucketName(), objectPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        // Create signed URL that expires in 15 minutes
        return storage.signUrl(blobInfo, 15, TimeUnit.MINUTES,
                        Storage.SignUrlOption.withV4Signature())
                .toString();
    }

    public String uploadDocument(byte[] documentData, String mimeType, String originalFilename) {
        try {
            // Create a unique filename using timestamp and original filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Dates.nowUTC());
            String uniqueFilename = timestamp + "_" + originalFilename;

            // Create document path in GCS
            String documentPath = "documents/" + uniqueFilename;

            BlobId blobId = BlobId.of(gcsProperties.getBucketName(), documentPath);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(mimeType)
                    .build();

            storage.create(blobInfo, documentData);
            return documentPath;

        } catch (Exception e) {
            logger.error("Storage error while uploading document: " + originalFilename, e);
            throw new RuntimeException("Failed to upload document to Google Cloud Storage", e);
        }
    }

    private String generateFileName(String originalFileName) {
        String extension = "";
        int lastDot = originalFileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFileName.substring(lastDot);
        }
        return UUID.randomUUID() + extension;
    }
}
