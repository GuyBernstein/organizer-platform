package com.organizer.platform.model.WhatsApp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Serves as the top-level container for webhook events, organizing multiple changes
 * that might occur in a single webhook call. This structure allows for batch processing
 * of updates and maintaining proper order of operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Entry {
    private String id;
    private List<Change> changes;
}
