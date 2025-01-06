package com.organizer.platform.model.WhatsApp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents audio content in WhatsApp messages, enabling the system to handle voice messages
 * and audio files. Voice messages need special handling due to their interactive nature and
 * potential for voice command processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Audio {
    private Boolean voice;
    @JsonProperty("mime_type")
    private String mimeType;
    private String sha256;
    private String id;
}