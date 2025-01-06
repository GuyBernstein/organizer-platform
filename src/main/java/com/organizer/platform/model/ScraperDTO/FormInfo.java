package com.organizer.platform.model.ScraperDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents an HTML form with its fields and attributes.
 * Used for capturing interactive elements that might need to be processed or filled.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormInfo {
    private String id;
    private String action;
    private String method;
    private List<FormField> fields;
}
