package com.organizer.platform.model.WhatsApp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Central class for message representation, supporting multiple content types and metadata.
 * This flexible structure allows the system to handle various message formats while maintaining
 * consistent processing patterns and enabling proper routing based on message type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String from;
    private String id;
    private String timestamp;
    private String type;
    private Text text;
    private Image image;
    private Document document;
    private Audio audio;
}
