package com.organizer.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Organizer Platform application.
 * <p>
 * This class bootstraps the entire Spring application context, enabling features like:
 * - Auto-configuration
 * - Component scanning
 * - External configuration processing
 */
@SpringBootApplication  // Combines @Configuration, @EnableAutoConfiguration, and @ComponentScan for simplified setup
public class OrganizerPlatformApplication {

	/**
	 * Main method serves as the application entry point.
	 * <p>
	 * Using SpringApplication.run() instead of manual context creation because it:
	 * - Handles both web and non-web environments automatically
	 * - Sets up reasonable defaults based on classpath
	 * - Provides a robust way to handle command-line arguments
	 * - Enables easy integration with external configuration sources
	 */
	public static void main(String[] args) {
		SpringApplication.run(OrganizerPlatformApplication.class, args);
	}

}
