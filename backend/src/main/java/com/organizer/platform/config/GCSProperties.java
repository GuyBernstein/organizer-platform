package com.organizer.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "gcp")
public class GCSProperties {
    private String bucketName;
    private String projectId;
    private String credentialsPath;
}
