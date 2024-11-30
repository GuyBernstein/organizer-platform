package com.organizer.platform.service.Scraper;

import com.organizer.platform.model.ScraperDTO.ProcessingResult;
import com.organizer.platform.model.ScraperDTO.TextBlock;
import com.organizer.platform.model.ScraperDTO.WebsiteContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ContentProcessorService {
    private static final String URL_REGEX = "https?://\\S+";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);
    private final WebContentScraperService scraperService;
    private static final Logger log = LoggerFactory.getLogger(ContentProcessorService.class);

    @Autowired
    public ContentProcessorService(WebContentScraperService scraperService) {
        this.scraperService = scraperService;
    }

    public ProcessingResult processContent(String content) {
        Matcher matcher = URL_PATTERN.matcher(content);

        // Check if there's exactly one URL
        if (countMatches(content) != 1) {
            return new ProcessingResult(content, null);  // Return original content if not exactly one URL
        }

        if (matcher.find()) {
            String url = matcher.group();
            return new ProcessingResult(content, scrapeUrl(url));
        }

        return new ProcessingResult(content, null);
    }

    private int countMatches(String content) {
        Matcher matcher = URL_PATTERN.matcher(content);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private String scrapeUrl(String url) {
        try {
            WebsiteContent websiteContent = scraperService.scrapeWebsite(url);
            List<String> visibleTexts = extractVisibleTexts(websiteContent);
            return String.join(",", visibleTexts);
        } catch (IOException e) {
            return "";
        }
    }

    private List<String> extractVisibleTexts(WebsiteContent websiteContent) {
        return websiteContent.getTextBlocks().stream()
                .filter(TextBlock::isVisible)
                .map(TextBlock::getText)
                .filter(text -> text != null && !text.trim().isEmpty())
                .collect(Collectors.toList());
    }

    private String formatScrapedContent(List<String> visibleTexts) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < visibleTexts.size(); i++) {
            if (i > 0) formatted.append("\n");
            formatted.append("Block ").append(i + 1).append(": ")
                    .append(visibleTexts.get(i).trim());
        }
        return formatted.toString();
    }
}
