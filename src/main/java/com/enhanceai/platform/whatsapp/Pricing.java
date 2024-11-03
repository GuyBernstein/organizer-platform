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
public class Pricing {
    private boolean billable;
    @JsonProperty("pricing_model")
    private String pricingModel;
    private String category;
}
