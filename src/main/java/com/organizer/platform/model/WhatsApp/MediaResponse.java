package com.organizer.platform.model.WhatsApp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse{
    private String url;
    private String mime_type;
    private String sha256;
    private Integer file_size;
    private String id;
    private String messaging_product;
}
