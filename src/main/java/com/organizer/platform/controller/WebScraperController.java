package com.organizer.platform.controller;

import com.organizer.platform.model.ScraperDTO.WebPageContent;
import com.organizer.platform.service.Scraper.WebScraperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RestController
@RequestMapping("/api/scraper")
@RequiredArgsConstructor
public class WebScraperController {

    private final WebScraperService webScraperService;

    @GetMapping("/fetch")
    public ResponseEntity<WebPageContent> scrapeWebPage(@RequestParam String url) {
        try {
            return ResponseEntity.ok(webScraperService.scrapeWebPage(url));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}