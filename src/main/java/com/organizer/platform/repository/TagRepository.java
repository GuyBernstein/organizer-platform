package com.organizer.platform.repository;

import com.organizer.platform.model.organizedDTO.Tag;
import com.organizer.platform.model.organizedDTO.WhatsAppMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for managing Tag entities.
 * Provides methods for querying and managing tags and their relationships with WhatsApp messages.
 *
 * @see Tag
 * @see WhatsAppMessage
 * @see JpaRepository
 */
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * Finds a tag by its name.
     *
     * @param name The name of the tag to find
     * @return An Optional containing the found Tag or empty if not found
     * @throws org.springframework.dao.DataAccessException if there's an error accessing the database
     */
    Optional<Tag> findByName(String name);

    /**
     * Retrieves all WhatsApp messages that have any of the specified tags.
     * Uses JOIN FETCH for optimal performance by avoiding N+1 query problems.
     *
     * @param tagNames Set of tag names to search for
     * @return Set of WhatsAppMessage entities that have any of the specified tags
     * @throws org.springframework.dao.DataAccessException if there's an error accessing the database
     */
    @Query("SELECT DISTINCT m FROM WhatsAppMessage m JOIN FETCH m.tags t WHERE t.name IN :tagNames")
    Set<WhatsAppMessage> findMessagesByTagNamesOptimized(@Param("tagNames") Set<String> tagNames);

    /**
     * Finds all tag names associated with messages from a specific phone number.
     *
     * @param phoneNumber The phone number to search tags for
     * @return Set of tag names associated with the specified phone number
     * @throws org.springframework.dao.DataAccessException if there's an error accessing the database
     */
    @Query("SELECT DISTINCT t.name FROM Tag t JOIN t.messages m WHERE m.fromNumber = :phoneNumber")
    Set<String> findTagNamesByPhoneNumber(@Param("phoneNumber") String phoneNumber);
}