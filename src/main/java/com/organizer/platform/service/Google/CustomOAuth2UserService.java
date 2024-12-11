package com.organizer.platform.service.Google;

import com.organizer.platform.model.User.AppUser;
import com.organizer.platform.service.User.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserService userService;

    @Autowired
    public CustomOAuth2UserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            OAuth2User oauth2User = super.loadUser(userRequest);

            // Extract email from Google OAuth2 response
            String email = oauth2User.getAttribute("email");
            if (email == null || email.isBlank()) {
                throw new OAuth2AuthenticationException("Email not found in OAuth2 response");
            }

            // Find or create our AppUser
            AppUser appUser = userService.findByEmail(email)
                    .orElseGet(() -> userService.save(createNewUser(oauth2User)));


            // Create authorities based on AppUser role
            Collection<GrantedAuthority> authorities = new HashSet<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()));

            // If user is authorized, add additional authority
            if (appUser.isAuthorized()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_AUTHORIZED"));
            }

            // Create a new OAuth2User with our additional info
            Map<String, Object> attributes = oauth2User.getAttributes();

            return new DefaultOAuth2User(
                    authorities,
                    attributes,
                    "email"
            );
        } catch (Exception e) {
            throw new OAuth2AuthenticationException("Error loading user");
        }
    }

    private AppUser createNewUser(OAuth2User oauth2User) {
        return userService.createUser(oauth2User);
    }
}