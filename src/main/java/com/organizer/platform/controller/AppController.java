package com.organizer.platform.controller;

import com.organizer.platform.model.WhatsAppMessage;
import com.organizer.platform.repository.WhatsAppMessageRepository;
import com.organizer.platform.service.CloudStorageService;
import com.organizer.platform.service.WhatsAppImageService;
import com.organizer.platform.service.WhatsAppMessageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content")
@Api(tags = "Content Management API")
public class AppController {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppImageService.class);
    private final WhatsAppMessageService messageService;
    private final CloudStorageService cloudStorageService;


    @Autowired
    public AppController(WhatsAppMessageService messageService, CloudStorageService cloudStorageService) {
        this.messageService = messageService;
        this.cloudStorageService = cloudStorageService;
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

    @GetMapping("/image/url")
    public ResponseEntity<?> getImagePreSignedUrl(
            @RequestParam() String imageName,
            HttpServletRequest request) {

        log.info("Request received: {} {}", request.getMethod(), request.getRequestURI());
        log.info("ImageName received: {}", imageName);

        try {
            String preSignedUrl = cloudStorageService.generateSignedUrl(imageName);
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", preSignedUrl);
            response.put("fileName", imageName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/document/url")
    public ResponseEntity<?> getDocumentPreSignedUrl(
            @RequestParam() String documentName,
            HttpServletRequest request) {

        log.info("Request received: {} {}", request.getMethod(), request.getRequestURI());
        log.info("DocumentName received: {}", documentName);

        try {
            // Generate pre-signed URL for the document
            String preSignedUrl = cloudStorageService.generateDocumentSignedUrl(documentName);

            // Create response with additional metadata
            Map<String, String> response = new HashMap<>();
            response.put("documentUrl", preSignedUrl);
            response.put("fileName", documentName);

            // Extract file extension for content type hints
            String fileExtension = getFileExtension(documentName);
            if (fileExtension != null) {
                response.put("fileType", fileExtension);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating document URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return null;
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : null;
    }
}