package com.organizer.platform.service.Scraper;

import com.organizer.platform.model.ScraperDTO.ProcessingResult;
import com.organizer.platform.model.ScraperDTO.TextBlock;
import com.organizer.platform.model.ScraperDTO.WebsiteContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service responsible for processing content that may contain URLs and extracting website content.
 * This service works in conjunction with WebContentScraperService to analyze and scrape web content.
 */
@Service
public class ContentProcessorService {
    // Regular expression for matching URLs (supports both http and https)
    private static final String URL_REGEX = "https?://\\S+";
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);

    // Service dependency for scraping website content
    private final WebContentScraperService scraperService;

    /**
     * Constructor for ContentProcessorService.
     * @param scraperService Injected service for scraping web content
     */
    @Autowired
    public ContentProcessorService(WebContentScraperService scraperService) {
        this.scraperService = scraperService;
    }

    /**
     * Processes input content by identifying URLs and scraping their content if exactly one URL is found.
     * If zero or multiple URLs are found, returns the original content without scraping.
     *
     * @param content The input text content to process
     * @return ProcessingResult containing original content and scraped content (if applicable)
     */
    public ProcessingResult processContent(String content) {
        Matcher matcher = URL_PATTERN.matcher(content);

        // Validate that there's exactly one URL in the content
        if (countMatches(content) != 1) {
            return new ProcessingResult(content, null);  // Return original content if not exactly one URL
        }

        // Extract and scrape the URL if found
        if (matcher.find()) {
            String url = matcher.group();
            return new ProcessingResult(content, scrapeUrl(url));
        }

        return new ProcessingResult(content, null);
    }

    /**
     * Counts the number of URLs present in the content.
     *
     * @param content The text content to analyze
     * @return The number of URLs found in the content
     */
    private int countMatches(String content) {
        Matcher matcher = URL_PATTERN.matcher(content);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    /**
     * Scrapes content from the provided URL using the WebContentScraperService.
     * Extracts visible text blocks and joins them with commas.
     *
     * @param url The URL to scrape
     * @return A comma-separated string of visible text from the webpage, or empty string if scraping fails
     */
    private String scrapeUrl(String url) {
        try {
            WebsiteContent websiteContent = scraperService.scrapeWebsite(url);
            List<String> visibleTexts = extractVisibleTexts(websiteContent);
            return String.join(",", visibleTexts);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Extracts visible text blocks from the scraped website content.
     * Filters out invisible elements and empty text blocks.
     *
     * @param websiteContent The scraped content from the website
     * @return List of visible text strings from the website
     */
    private List<String> extractVisibleTexts(WebsiteContent websiteContent) {
        return websiteContent.getTextBlocks().stream()
                .filter(TextBlock::isVisible)
                .map(TextBlock::getText)
                .filter(text -> text != null && !text.trim().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Formats the scraped content into a readable string with block numbers.
     * Note: This method is currently unused but kept for potential future use.
     *
     * @param visibleTexts List of visible text blocks to format
     * @return Formatted string with numbered blocks
     */
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