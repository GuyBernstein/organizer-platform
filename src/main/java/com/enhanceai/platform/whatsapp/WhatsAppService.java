package com.enhanceai.platform.whatsapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WhatsAppService {

    public void processMessage(String messageContent, String senderPhoneNumber, String messageId) {
        log.info("Processing message: content={}, sender={}, messageId={}",
                messageContent, senderPhoneNumber, messageId);

        // Add your business logic here to process the message
        // For example:
        // - Store in database
        // - Trigger other services
        // - Send notifications
        // - Generate responses
    }
}
