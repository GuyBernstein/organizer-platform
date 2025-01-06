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

/**
 * Service class for handling Google Cloud Storage operations.
 * This service provides functionality to upload and manage different types of media files
 * (images, documents, and audio) in Google Cloud Storage.
 * Each media type is stored in its own directory structure with user-specific subdirectories.
 */
@Service
public class CloudStorageService {
    private final GCSProperties gcsProperties;
    private Storage storage;
    private Bucket bucket;

    /**
     * Constructs the CloudStorageService with necessary GCS properties.
     * @param gcsProperties Configuration properties for Google Cloud Storage
     */
    @Autowired
    public CloudStorageService(GCSProperties gcsProperties) {
        this.gcsProperties = gcsProperties;
    }

    /**
     * Initializes the Google Cloud Storage client and validates bucket access.
     * This method is called automatically after bean construction.
     * @throws IOException if there's an error reading credentials or accessing the bucket
     */
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

    /**
     * Generates a signed URL for accessing an image file.
     * @param fromNumber User identifier or phone number
     * @param imageName Name of the image file
     * @return Signed URL with temporary access to the image
     */
    public String generateImageSignedUrl(String fromNumber, String imageName) {
        return generateSignedUrlForObject("images/" + fromNumber + "/" + imageName);
    }

    /**
     * Generates a signed URL for accessing a document file.
     * @param fromNumber User identifier or phone number
     * @param documentName Name of the document file
     * @return Signed URL with temporary access to the document
     */
    public String generateDocumentSignedUrl(String fromNumber, String documentName) {
        return generateSignedUrlForObject("documents/" + fromNumber + "/" + documentName);
    }

    /**
     * Generates a signed URL for accessing an audio file.
     * @param fromNumber User identifier or phone number
     * @param audioName Name of the audio file
     * @return Signed URL with temporary access to the audio file
     */
    public String generateAudioSignedUrl(String fromNumber, String audioName) {
        return generateSignedUrlForObject("audios/" + fromNumber + "/" + audioName);
    }

    /**
     * Generates a signed URL for any object in the storage bucket.
     * The URL expires after 15 minutes.
     * @param objectPath Full path to the object in the bucket
     * @return Temporary signed URL for accessing the object
     */
    private String generateSignedUrlForObject(String objectPath) {
        BlobId blobId = BlobId.of(gcsProperties.getBucketName(), objectPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        // Create signed URL that expires in 15 minutes
        return storage.signUrl(blobInfo, 15, TimeUnit.MINUTES,
                        Storage.SignUrlOption.withV4Signature())
                .toString();
    }

    /**
     * Uploads a document to Google Cloud Storage.
     * The document is stored in a user-specific directory with a timestamp-based filename.
     * @param fromNumber User identifier or phone number
     * @param documentData Binary content of the document
     * @param mimeType MIME type of the document
     * @param originalFilename Original name of the uploaded file
     * @return Path to the uploaded document in GCS
     * @throws RuntimeException if upload fails
     */
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

    /**
     * Uploads an image to Google Cloud Storage.
     * The image is stored in a user-specific directory with a UUID-based filename.
     * @param fromNumber User identifier or phone number
     * @param imageData Binary content of the image
     * @param mimeType MIME type of the image
     * @param originalFileName Original name of the uploaded file
     * @return Path to the uploaded image in GCS
     */
    public String uploadImage(String fromNumber, byte[] imageData, String mimeType, String originalFileName) {
        // Generates a unique filename using UUID
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

    /**
     * Uploads an audio file to Google Cloud Storage.
     * The audio is stored in a user-specific directory with a timestamp-based filename.
     * @param fromNumber User identifier or phone number
     * @param audioData Binary content of the audio file
     * @param mimeType MIME type of the audio file
     * @param originalFileName Original name of the uploaded file
     * @return Path to the uploaded audio file in GCS
     * @throws RuntimeException if upload fails
     */
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

    /**
     * Generates a unique filename by appending a UUID to the original file extension.
     * @param originalFileName Original name of the uploaded file
     * @return New filename with UUID and original extension
     */
    private String generateFileName(String originalFileName) {
        String extension = "";
        int lastDot = originalFileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFileName.substring(lastDot);
        }
        return UUID.randomUUID() + extension;
    }
}