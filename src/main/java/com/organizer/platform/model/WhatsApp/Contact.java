package com.organizer.platform.model.WhatsApp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stores WhatsApp contact information, crucial for maintaining user context and enabling
 * proper message routing. The WhatsApp ID (waId) is particularly important as it serves
 * as a stable identifier for user tracking across conversations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contact {
    private Profile profile;
    @JsonProperty("wa_id")
    private String waId;
}
