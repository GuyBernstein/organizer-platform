package com.organizer.platform.model.AI;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Tracks token usage statistics for AI model interactions.
 * Used for monitoring resource consumption and potentially for billing purposes.
 */
@Getter
public class Usage {
    @JsonProperty("input_tokens")
    private Integer inputTokens;     // Number of tokens in the input/prompt

    @JsonProperty("output_tokens")
    private Integer outputTokens;    // Number of tokens generated in the response
}