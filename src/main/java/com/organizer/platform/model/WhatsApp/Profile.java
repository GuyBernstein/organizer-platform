package com.organizer.platform.model.WhatsApp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stores user profile information, keeping user-related data separate from technical
 * identifiers. This separation allows for easier user information management and updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile {
    private String name;
}
