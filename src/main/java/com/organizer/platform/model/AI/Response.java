package com.organizer.platform.model.AI;

import lombok.Getter;

import java.util.List;

@Getter
public class Response{
    private String id;
    private String type;
    private String role;
    private String model;
    private List<Content> content;
    private String stop_reason;
    private String stop_sequence;
    private Usage usage;

}