package com.organizer.platform.model.AI;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Represents a complete response from an AI model.
 * Contains all metadata about the response including the model used,
 * content generated, and usage statistics.
 */
@Getter
public class Response {
    private String id;              // Unique identifier for the response
    private String type;            // Type of the response
    private String role;            // Role of the AI (e.g., "assistant")
    private String model;           // Name/version of the AI model used
    private List<Content> content;  // List of content elements in the response

    @JsonProperty("stop_reason")
    private String stopReason;      // Reason why the AI stopped generating

    @JsonProperty("stop_sequence")
    private String stopSequence;    // Sequence that caused the AI to stop

    private Usage usage;            // Token usage statistics for the response
}