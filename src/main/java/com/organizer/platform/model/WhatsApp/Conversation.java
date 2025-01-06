package com.organizer.platform.model.WhatsApp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Manages conversation metadata and lifecycle, helping track conversation expiration
 * and origin. This is essential for implementing conversation timeouts and maintaining
 * compliance with WhatsApp's conversation window requirements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    private String id;
    @JsonProperty("expiration_timestamp")
    private String expirationTimestamp;
    private Origin origin;
}
