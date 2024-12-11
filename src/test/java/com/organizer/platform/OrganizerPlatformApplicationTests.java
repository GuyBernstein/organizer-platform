package com.organizer.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class OrganizerPlatformApplicationTests {

	@Autowired
	private JmsTemplate jmsTemplate;

	@Test
	void contextLoads() {
		// Original context load test
	}

	@Test
	void jmsConfigurationTest() {
		assertNotNull(jmsTemplate, "JmsTemplate should be configured");
	}
}
