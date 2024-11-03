package com.enhanceai.platform.whatsapp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
