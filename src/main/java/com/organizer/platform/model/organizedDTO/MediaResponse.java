package com.organizer.platform.model.organizedDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing the response from WhatsApp's Media API.
 * This class maps the JSON response received when requesting media information
 * from WhatsApp's servers using the Graph API v18.0.
 *
 * Fields:
 * - url: The temporary URL where the media can be downloaded
 * - mime_type: The MIME type of the media file (e.g., "image/jpeg", "audio/mp3")
 * - sha256: Hash of the file for integrity verification
 * - file_size: Size of the media file in bytes
 * - id: Unique identifier for the media
 * - messaging_product: Type of messaging product (typically "whatsapp")
 */
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
