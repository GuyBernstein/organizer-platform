package com.enhanceai.platform.whatsapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class Message {
    private String from;
    private String id;
    private String timestamp;
    private Text text;
    private String type;
}
