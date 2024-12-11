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

@Configuration
public class GoogleOAuthConfig {
    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private final OAuthProperties oAuthProperties;

    public GoogleOAuthConfig(OAuthProperties oAuthProperties) {
        this.oAuthProperties = oAuthProperties;
    }

    @PostConstruct
    public void initialize() {
        if (!StringUtils.hasText(oAuthProperties.getCredentialsPath())) {
            throw new IllegalStateException("OAuth credentials path not configured");
        }
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() throws IOException {
        Resource resource = new ClassPathResource(
                oAuthProperties.getCredentialsPath().replace("classpath:", "")
        );

        if (!resource.exists()) {
            throw new IllegalStateException("OAuth credentials file not found: " + oAuthProperties.getCredentialsPath());
        }

        JsonNode jsonNode = new ObjectMapper()
                .readTree(resource.getInputStream())
                .get("web");

        if (jsonNode == null || !jsonNode.has("client_id") || !jsonNode.has("client_secret")) {
            throw new IllegalStateException("Invalid OAuth credentials format");
        }

        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId(jsonNode.get("client_id").asText())
                .clientSecret(jsonNode.get("client_secret").asText())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v4/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .jwkSetUri(GOOGLE_JWK_SET_URI)
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Google")
                .build();

        return new InMemoryClientRegistrationRepository(registration);
    }
}