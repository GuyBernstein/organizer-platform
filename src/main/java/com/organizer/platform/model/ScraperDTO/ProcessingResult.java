package com.organizer.platform.model.ScraperDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Container for both original and processed content.
 * Used to track transformations and maintain reference to original data.
 */@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingResult {
    private String originalContent;
    private String scrapedContent;
}
