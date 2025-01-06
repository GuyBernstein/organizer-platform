package com.organizer.platform.model.AI;

import lombok.Getter;

/**
 * Represents a single content element in an AI response.
 * Used to structure different types of content (e.g., text, images)
 * that can be part of an AI's response.
 */
@Getter
public class Content {
    private String type;    // The type of content (e.g., "text")
    private String text;    // The actual content data
}
