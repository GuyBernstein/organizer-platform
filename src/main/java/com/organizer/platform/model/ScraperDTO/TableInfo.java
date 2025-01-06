package com.organizer.platform.model.ScraperDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a table structure from the webpage.
 * Maintains the table's structure with headers and data rows for structured data extraction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableInfo {
    private List<String> headers;
    private List<List<String>> rows;
    private String caption;
}
