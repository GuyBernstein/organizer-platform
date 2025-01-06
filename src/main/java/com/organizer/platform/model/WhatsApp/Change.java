package com.organizer.platform.model.WhatsApp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Acts as a container for webhook event changes, allowing the system to track what type of
 * update occurred (messages, status updates, etc.) and process them accordingly. This separation
 * helps maintain clean event handling and routing logic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Change {
    private Value value;
    private String field;
}
