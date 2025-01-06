package com.organizer.platform.model.WhatsApp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tracks message delivery and read status, enabling proper message flow monitoring
 * and implementing retry logic when needed. The conversation and pricing information
 * help maintain proper business logic and cost tracking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Status {
    private String id;
    private String status;
    private String timestamp;
    @JsonProperty("recipient_id")
    private String recipientId;
    private Conversation conversation;
    private Pricing pricing;
}
