package com.organizer.platform.model.WhatsApp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stores essential phone number information for message routing and display.
 * Separating display and ID numbers allows for proper handling of different phone
 * number formats and maintains clean separation between user-facing and system-internal identifiers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metadata {
    @JsonProperty("display_phone_number")
    private String displayPhoneNumber;
    @JsonProperty("phone_number_id")
    private String phoneNumberId;
}
