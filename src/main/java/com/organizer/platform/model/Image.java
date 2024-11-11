package com.organizer.platform.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Image {
    @JsonProperty("mime_type")
    private String mimeType;
    private String sha256;
    private String id;
}
