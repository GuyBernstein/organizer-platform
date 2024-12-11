package com.organizer.platform.model.ScraperDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Result class to hold both original content and scraped content
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingResult {
    private String originalContent;
    private String scrapedContent;
}
