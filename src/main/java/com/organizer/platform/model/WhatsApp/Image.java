package com.organizer.platform.model.WhatsApp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Manages image attachments in messages, similar to Document but specialized for image handling.
 * The SHA256 hash enables integrity verification and potential caching strategies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Image {
    @JsonProperty("mime_type")
    private String mimeType;
    private String sha256;
    private String id;
}
