package com.organizer.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger configuration for API documentation.
 * Note: Login-related endpoints are exposed in the API documentation but
 * actual authentication is handled by OAuth2 as configured in SecurityConfig.
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                // Document all endpoints in the platform package, including auth endpoints
                .apis(RequestHandlerSelectors.basePackage("com.organizer.platform"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo())
                .useDefaultResponseMessages(false);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("WhatsApp Organizer API")
                .description("WhatsApp message organization and processing API")
                .version("1.0")
                .contact(new Contact("Guy Bernstein", "www.tapitim.com", "guyu669@gmail.com"))
                .license("License of API")
                .licenseUrl("API license URL")
                .build();
    }

    // RestTemplate bean used for OAuth2 token exchange and API calls
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}