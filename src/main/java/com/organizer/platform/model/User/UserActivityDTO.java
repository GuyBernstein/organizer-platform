package com.organizer.platform.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.LocalDateTime;

import java.util.Map;

/**
 * // UserActivityDTO represents user engagement metrics for analytics purposes.
 * // It aggregates message activity data by user over time, useful for tracking user participation
 * // and generating activity reports.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityDTO {
    // Reference to the user whose activity is being tracked
    private Long userId;
    // Display name of the user for readable reports
    private String username;
    // Tracks number of messages sent by this user at specific timestamps
    // Key: Timestamp of activity
    // Value: Count of messages at that time
    private Map<LocalDateTime, Long> messageCountByDate;
}