package com.organizer.platform.service.Google;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException{
        OAuth2User user = super.loadUser(userRequest);

        // Extract email from Google OAuth2 response
        String email = user.getAttribute("email");
        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found in OAuth2 response");
        }

        // Create a new set of authorities
        Set<GrantedAuthority> authorities = new HashSet<>(user.getAuthorities());

        // Add ROLE_USER by default
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // Add ADMIN role for specific email
        if ("guyu669@gmail.com".equalsIgnoreCase(email.trim())){
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return new DefaultOAuth2User(
                authorities,
                user.getAttributes(),
                "email"
        );

    }
}