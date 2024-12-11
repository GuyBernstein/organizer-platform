package com.organizer.platform.service.Google;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.organizer.platform.config.GCSProperties;
import com.organizer.platform.util.Dates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CloudStorageService {
    private final GCSProperties gcsProperties;
    private Storage storage;
    private Bucket bucket;

    @Autowired
    public CloudStorageService(GCSProperties gcsProperties) {
        this.gcsProperties = gcsProperties;
    }

    @PostConstruct
    public void initialize() throws IOException {
        Resource resource = new ClassPathResource(gcsProperties.getCredentialsPath().replace("classpath:", ""));
        GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream());

        storage = StorageOptions.newBuilder()
                .setProjectId(gcsProperties.getProjectId())
                .setCredentials(credentials)
                .build()
                .getService();

        // Test bucket access
        bucket = storage.get(gcsProperties.getBucketName());
        if (bucket == null) {
            throw new IllegalStateException("Bucket not found: " + gcsProperties.getBucketName());
        }
    }

    public String generateImageSignedUrl(String fromNumber, String imageName) {
        return generateSignedUrlForObject("images/" + fromNumber + "/" + imageName);
    }

    public String generateDocumentSignedUrl(String fromNumber, String documentName) {
        return generateSignedUrlForObject("documents/" + fromNumber + "/" + documentName);
    }

    public String generateAudioSignedUrl(String fromNumber, String audioName) {
        return generateSignedUrlForObject("audios/" + fromNumber + "/" + audioName);
    }

    private String generateSignedUrlForObject(String objectPath) {
        BlobId blobId = BlobId.of(gcsProperties.getBucketName(), objectPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        // Create signed URL that expires in 15 minutes
        return storage.signUrl(blobInfo, 15, TimeUnit.MINUTES,
                        Storage.SignUrlOption.withV4Signature())
                .toString();
    }

    public String uploadDocument(String fromNumber, byte[] documentData, String mimeType, String originalFilename) {
        try {
            // Create a unique filename using timestamp and original filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Dates.nowUTC());
            String uniqueFilename = timestamp + "_" + originalFilename;

            // Create document path in GCS with fromNumber prefix
            String documentPath = "documents/" + fromNumber + "/" + uniqueFilename;

            BlobId blobId = BlobId.of(gcsProperties.getBucketName(), documentPath);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(mimeType)
                    .build();

            storage.create(blobInfo, documentData);
            return documentPath;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload document to Google Cloud Storage", e);
        }
    }

    public String uploadImage(String fromNumber, byte[] imageData, String mimeType, String originalFileName) {
        // Generates a unique filename
        String fileName = generateFileName(originalFileName);

        // Create image path in GCS with fromNumber prefix
        String imagePath = "images/" + fromNumber + "/" + fileName;

        // Creates a blob (file) in GCS
        BlobId blobId = BlobId.of(gcsProperties.getBucketName(), imagePath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(mimeType)
                .build();

        // Uploads the file
        storage.create(blobInfo, imageData);
        return imagePath;
    }

    public String uploadAudio(String fromNumber, byte[] audioData, String mimeType, String originalFileName) {
        try {
            // Generate a unique filename using timestamp and original filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Dates.nowUTC());
            String uniqueFilename = timestamp + "_" + originalFileName;

            // Create audio path in GCS under 'audio' directory with fromNumber prefix
            String audioPath = "audios/" + fromNumber + "/" + uniqueFilename;

            // Create blob (file) in GCS
            BlobId blobId = BlobId.of(gcsProperties.getBucketName(), audioPath);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(mimeType)
                    .build();

            // Upload the file
            storage.create(blobInfo, audioData);

            return audioPath;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload audio to Google Cloud Storage", e);
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