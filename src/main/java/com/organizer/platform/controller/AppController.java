package com.organizer.platform.controller;

import com.organizer.platform.model.WhatsAppMessage;
import com.organizer.platform.repository.WhatsAppMessageRepository;
import com.organizer.platform.service.WhatsAppMessageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content")
@Api(tags = "Content Management API")
public class AppController {
    private final WhatsAppMessageService messageService;

    @Autowired
    public AppController(WhatsAppMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/messages/{phoneNumber}")
    @ApiOperation(value = "Get message contents by phone number",
            notes = "Retrieves all message contents sent from a specific phone number. Accepts formats: 0509603888 or 972509603888")
    public ResponseEntity<List<String>> getMessageContentsByPhoneNumber(
            @PathVariable String phoneNumber) {

        // Validate input phone number format and convert to international format
        String internationalFormat;

        // Check if phone starts with 0 (0509603888)
        if (phoneNumber.matches("^05\\d{8}$")) {
            internationalFormat = "972" + phoneNumber.substring(1);
        }
        // Check if phone starts with 972 (972509603888)
        else if (phoneNumber.matches("^972\\d{9}$")) {
            internationalFormat = phoneNumber;
        }
        // Invalid format
        else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        List<String> messageContents = messageService.findMessageContentsByFromNumber(internationalFormat);

        if (messageContents.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(messageContents, HttpStatus.OK);
    }
}