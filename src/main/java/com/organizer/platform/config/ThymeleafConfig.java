package com.organizer.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.extras.springsecurity5.dialect.SpringSecurityDialect;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Thymeleaf configuration for template rendering.
 * Includes security dialect for handling authentication state in views.
 */
@Configuration
public class ThymeleafConfig {
    @Bean
    public SpringResourceTemplateResolver templateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        // Configure template location - includes login templates
        templateResolver.setPrefix("classpath:/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        // Set UTF-8 encoding for proper handling of international characters
        templateResolver.setCharacterEncoding("UTF-8");
        // Enable template caching for better performance
        templateResolver.setCacheable(true);
        templateResolver.setCheckExistence(true);
        return templateResolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine(SpringResourceTemplateResolver templateResolver) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        templateEngine.setEnableSpringELCompiler(true);
        // Add Spring Security dialect for auth-aware templates
        // This enables security features in Thymeleaf templates like:
        // - sec:authorize for conditional rendering based on auth status
        // - sec:authentication for accessing authentication details
        templateEngine.addDialect(new SpringSecurityDialect());
        return templateEngine;
    }
}