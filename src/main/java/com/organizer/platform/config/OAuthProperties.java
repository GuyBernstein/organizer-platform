package com.organizer.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties class for OAuth settings.
 * Maps properties from application.yml/properties with the prefix "oauth".
 * For example: oauth.credentials-path=classpath:/google-credentials.json
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {
    /**
     * Path to the OAuth credentials file.
     * This should point to a JSON file containing OAuth client credentials.
     * Can be specified using classpath: prefix for resources in the classpath.
     * Example value: "classpath:/google-credentials.json"
     */
    private String credentialsPath;
}

