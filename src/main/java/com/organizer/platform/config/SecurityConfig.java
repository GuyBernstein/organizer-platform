package com.organizer.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Security configuration for the application.
 * Handles authentication, authorization, and OAuth2 login settings.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // Disable CSRF protection as we're using OAuth2
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeRequests(authz -> authz
                        // Public endpoints that don't require authentication
                        .requestMatchers(
                                // Landing page and login
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/login"),
                                // Static resources accessible without auth
                                new AntPathRequestMatcher("/static/**"),
                                new AntPathRequestMatcher("/css/**"),
                                new AntPathRequestMatcher("/js/**"),
                                new AntPathRequestMatcher("/images/**"),
                                new AntPathRequestMatcher("/fonts/**"),
                                new AntPathRequestMatcher("/favicon.ico"),
                                // OAuth2 authentication endpoints
                                new AntPathRequestMatcher("/oauth2/**"),
                                new AntPathRequestMatcher("/login/oauth2/code/*"),
                                new AntPathRequestMatcher("/auth-status"),
                                // WhatsApp webhook endpoint
                                new AntPathRequestMatcher("/webhook"),
                                // Swagger documentation endpoints
                                new AntPathRequestMatcher("/swagger-ui/**"),
                                new AntPathRequestMatcher("/v3/api-docs/**"),
                                new AntPathRequestMatcher("/v2/api-docs"),
                                new AntPathRequestMatcher("/swagger-resources/**"),
                                new AntPathRequestMatcher("/webjars/**")
                        ).permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                // Configure OAuth2 login
                .oauth2Login()
                // Custom login page path
                .loginPage("/login")
                // Redirect after successful login
                .defaultSuccessUrl("/login", true)
                .and()
                // Configure logout behavior
                .logout(logout -> logout
                        // Redirect to login page with logout parameter
                        .logoutSuccessUrl("/login?logout=true")
                        // Clear session data
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .permitAll()
                )
                .build();
    }
}