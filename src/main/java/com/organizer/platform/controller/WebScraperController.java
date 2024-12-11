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

@RestController
@RequestMapping("/api/scraper")
@RequiredArgsConstructor
public class WebScraperController {

    private final WebContentScraperService scraperService;
    @GetMapping("/analyze")
    public ResponseEntity<WebsiteContent> analyzeWebsite(@RequestParam String url) {
        try {
            return ResponseEntity.ok(scraperService.scrapeWebsite(url));
        } catch (IOException e) {
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
            return ResponseEntity.badRequest().build();
        }
    }
}