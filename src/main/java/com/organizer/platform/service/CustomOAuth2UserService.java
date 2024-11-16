package com.organizer.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            logger.debug("OAuth2 Authorization Request: {}", userRequest.getClientRegistration().getRegistrationId());
            logger.debug("Access Token Value: {}", userRequest.getAccessToken().getTokenValue());
            logger.debug("Access Token Scopes: {}", userRequest.getAccessToken().getScopes());

            OAuth2User user = super.loadUser(userRequest);

            logger.debug("User Attributes: {}", user.getAttributes());
            logger.debug("User Authorities: {}", user.getAuthorities());

            return user;
        } catch (Exception e) {
            logger.error("Error loading OAuth2 user", e);
            throw e;
        }
    }
}