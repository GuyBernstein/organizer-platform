package com.organizer.platform.model.WhatsApp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Manages pricing information for messages, enabling cost tracking and billing features.
 * This information is crucial for maintaining proper business operations and implementing
 * usage-based pricing models.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pricing {
    private boolean billable;
    @JsonProperty("pricing_model")
    private String pricingModel;
    private String category;
}
