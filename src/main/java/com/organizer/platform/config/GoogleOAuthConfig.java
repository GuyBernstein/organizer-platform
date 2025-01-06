package com.organizer.platform.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Configuration class for Google OAuth2 authentication.
 * This class sets up the necessary beans and configuration for enabling
 * Google OAuth2 login in the application.
 */
@Configuration
public class GoogleOAuthConfig {
    // URI for Google's JSON Web Key Set, used for verifying JWT tokens
    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    // Properties containing OAuth configuration, including credentials path
    private final OAuthProperties oAuthProperties;

    /**
     * Constructor that injects the OAuth properties.
     * @param oAuthProperties Configuration properties for OAuth
     */
    public GoogleOAuthConfig(OAuthProperties oAuthProperties) {
        this.oAuthProperties = oAuthProperties;
    }

    /**
     * Initializes the configuration and validates the credentials path.
     * This method runs after bean construction to ensure required properties are set.
     * @throws IllegalStateException if credentials path is not configured
     */
    @PostConstruct
    public void initialize() {
        if (!StringUtils.hasText(oAuthProperties.getCredentialsPath())) {
            throw new IllegalStateException("OAuth credentials path not configured");
        }
    }

    /**
     * Creates and configures the ClientRegistrationRepository bean for Google OAuth2.
     * This bean is essential for Spring Security OAuth2 client registration.
     *
     * @return ClientRegistrationRepository containing Google OAuth2 client registration
     * @throws IOException if there's an error reading the credentials file
     * @throws IllegalStateException if credentials file is not found or has invalid format
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() throws IOException {
        // Load credentials file from classpath
        Resource resource = new ClassPathResource(
                oAuthProperties.getCredentialsPath().replace("classpath:", "")
        );

        // Verify credentials file exists
        if (!resource.exists()) {
            throw new IllegalStateException("OAuth credentials file not found: " + oAuthProperties.getCredentialsPath());
        }

        // Parse the credentials JSON file
        JsonNode jsonNode = new ObjectMapper()
                .readTree(resource.getInputStream())
                .get("web");

        // Validate required credentials are present
        if (jsonNode == null || !jsonNode.has("client_id") || !jsonNode.has("client_secret")) {
            throw new IllegalStateException("Invalid OAuth credentials format");
        }

        // Build the client registration with Google OAuth2 configuration
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId(jsonNode.get("client_id").asText())
                .clientSecret(jsonNode.get("client_secret").asText())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                // Configure the redirect URI using Spring's placeholder format
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                // Request standard OAuth2 scopes for basic profile information
                .scope("openid", "profile", "email")
                // Configure Google OAuth2 endpoints
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .jwkSetUri(GOOGLE_JWK_SET_URI)
                // Configure the user identifier claim from the ID token
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Google")
                .build();

        // Create and return an in-memory repository with the Google registration
        return new InMemoryClientRegistrationRepository(registration);
    }
}