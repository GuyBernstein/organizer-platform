package com.organizer.platform.model.ScraperDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a block of text content with its HTML context.
 * Preserves the structural and styling information of text elements for accurate content processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextBlock {
    private String text;
    private String tag;
    private String cssClass;
    private String id;
    private int depth;
    private String parentTags;
    private boolean isVisible;
}
