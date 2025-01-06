package com.organizer.platform.model.ScraperDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Base model for a website's scraped content. Acts as a container for all
 * extracted elements like text, tables, forms, and metadata from a web page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsiteContent {
    private String title;
    private String description;
    private Map<String, String> metadata;
    private List<TextBlock> textBlocks;
    private List<String> scripts;
    private List<String> styles;
    private Map<String, List<String>> lists;
    private List<TableInfo> tables;
    private List<FormInfo> forms;
}
