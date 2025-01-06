package com.organizer.platform.model.WhatsApp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Handles document attachments in messages, providing structure for file sharing capabilities.
 * The SHA256 hash is included for security verification and deduplication purposes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    private String filename;
    @JsonProperty("mime_type")
    private String mimeType;
    private String sha256;
    private String id;
}
