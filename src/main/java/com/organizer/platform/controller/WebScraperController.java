package com.organizer.platform.controller;

import com.organizer.platform.model.ScraperDTO.WebsiteContent;
import com.organizer.platform.service.Scraper.WebContentScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import com.organizer.platform.model.ScraperDTO.TextBlock;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/scraper")
@RequiredArgsConstructor
public class WebScraperController {

    private final WebContentScraperService scraperService;
    Logger log = LoggerFactory.getLogger(WebScraperController.class);

    @GetMapping("/analyze")
    public ResponseEntity<WebsiteContent> analyzeWebsite(@RequestParam String url) {
        try {
            return ResponseEntity.ok(scraperService.scrapeWebsite(url));
        } catch (IOException e) {
            log.error("Failed to analyze website: {}", url, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/visibleText")
    public ResponseEntity<String> textWebsite(@RequestParam String url) {
        try {
            WebsiteContent websiteContent = scraperService.scrapeWebsite(url);
            List<String> visibleText = websiteContent.getTextBlocks().stream()
                    .filter(TextBlock::isVisible)
                    .map(TextBlock::getText)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(String.join(",",visibleText));
        } catch (IOException e) {
            log.error("Failed to analyze website: {}", url, e);
            return ResponseEntity.badRequest().build();
        }
    }
}