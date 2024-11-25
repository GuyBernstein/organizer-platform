package com.organizer.platform.model.AI;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
public class Response{
    private String id;
    private String type;
    private String role;
    private String model;
    private List<Content> content;
    @JsonProperty("stop_reason")
    private String stopReason;

    @JsonProperty("stop_sequence")
    private String stopSequence;
    private Usage usage;
}