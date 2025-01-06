package com.organizer.platform.model.WhatsApp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Serves as the root entry point for all incoming WhatsApp webhook requests, providing
 * a standardized structure for initial request validation and routing. This class is crucial because:
 * - It enables quick verification of webhook authenticity through the 'object' field
 * - It organizes multiple entries into a single batch, allowing for efficient bulk processing
 * - It serves as the first line of defense in the webhook handling pipeline, ensuring
 *   proper request structure before deeper processing begins
 * - It maintains a clean separation between webhook payload parsing and business logic processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppWebhookRequest {
    private String object;
    private List<Entry> entry;
}
