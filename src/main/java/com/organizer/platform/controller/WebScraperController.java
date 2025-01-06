package com.organizer.platform.controller;

import com.organizer.platform.model.ScraperDTO.TextBlock;
import com.organizer.platform.model.ScraperDTO.WebsiteContent;
import com.organizer.platform.service.Scraper.WebContentScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enhances AI interactions by automatically processing URLs mentioned in user messages.
 * Extracts website content to provide AI with context about referenced URLs,
 * enabling more informed and contextual responses.
 */
@RestController
@RequestMapping("/api/scraper")
@RequiredArgsConstructor
public class WebScraperController {
    private final WebContentScraperService scraperService;

    /**
     * Provides structured website content for AI processing.
     * Captures both visible and hidden elements to give AI complete context
     * about the referenced webpage's structure and content hierarchy.
     *
     * @param url URL mentioned in user's message
     * @return Structured content for AI context enhancement
     */
    @GetMapping("/analyze")
    public ResponseEntity<WebsiteContent> analyzeWebsite(@RequestParam String url) {
        try {
            return ResponseEntity.ok(scraperService.scrapeWebsite(url));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Extracts only readable content for simplified AI processing.
     * Filters to visible text for cases where webpage structure isn't relevant,
     * optimizing for pure text-based AI analysis.
     *
     * @param url URL mentioned in user's message
     * @return Visible text content for AI context enhancement
     */
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
            return ResponseEntity.badRequest().build();
        }
    }
}