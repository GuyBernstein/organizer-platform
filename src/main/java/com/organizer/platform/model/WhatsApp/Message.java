package com.organizer.platform.model.WhatsApp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
