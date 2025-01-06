package com.organizer.platform.model.WhatsApp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Acts as a container for the actual webhook payload, organizing different types of
 * updates (messages, contacts, statuses) in a structured way. This organization enables
 * proper event routing and maintains clean separation between different types of updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Value {
    @JsonProperty("messaging_product")
    private String messagingProduct;
    private Metadata metadata;
    private List<Contact> contacts;
    private List<Message> messages;
    private List<Status> statuses;
}
