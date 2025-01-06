package com.organizer.platform.model.WhatsApp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tracks the origin of conversations, helping implement different handling logic
 * based on how the conversation started. This is crucial for applying appropriate
 * business rules and maintaining conversation context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Origin {
    private String type;

}
