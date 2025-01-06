package com.organizer.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Google Cloud Storage.
 * This class maps properties with the 'gcp' prefix from the application configuration.
 * Used to configure the connection to Google Cloud Storage service.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "gcp")
public class GCSProperties {
    /**
     * Name of the Google Cloud Storage bucket to be used
     */
    private String bucketName;

    /**
     * Google Cloud Project ID
     */
    private String projectId;

    /**
     * Path to the Google Cloud credentials file
     * Expected to be in the classpath
     */
    private String credentialsPath;
}