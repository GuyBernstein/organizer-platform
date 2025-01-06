package com.organizer.platform.model.WhatsApp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents text content in messages, separated from other content types to maintain
 * clean separation of concerns and enable specialized text processing when needed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Text {
    private String body;
}
