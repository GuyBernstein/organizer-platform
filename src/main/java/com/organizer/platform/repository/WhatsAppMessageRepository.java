package com.organizer.platform.repository;

import com.organizer.platform.model.organizedDTO.Tag;
import com.organizer.platform.model.organizedDTO.WhatsAppMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

/**
 * Repository interface for managing WhatsAppMessage entities.
 * Provides methods for querying and analyzing WhatsApp messages including relationship with tags
 * and message content search functionality.
 *
 * @see WhatsAppMessage
 * @see Tag
 * @see JpaRepository
 */
public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {

    /**
     * Retrieves all messages from a specific phone number.
     *
     * @param fromNumber The sender's phone number
     * @return List of WhatsAppMessage entities from the specified number
     * @throws org.springframework.dao.DataAccessException if there's an error accessing the database
     */
    List<WhatsAppMessage> findByFromNumber(String fromNumber);

    /**
     * Finds related messages that share common tags with a specific message.
     * Uses JOIN operation for efficient querying and excludes the original message.
     *
     * @param tags Set of Tag entities to search for
     * @param messageId ID of the message to exclude from results
     * @return List of related WhatsAppMessage entities
     * @throws org.springframework.dao.DataAccessException if there's an error accessing the database
     */
    @Query("SELECT DISTINCT m " +
            "FROM WhatsAppMessage m JOIN m.tags t " +
            "WHERE t IN :tags AND m.id != :messageId")
    List<WhatsAppMessage> findRelatedMessagesByTags(@Param("tags") Set<Tag> tags, @Param("messageId") Long messageId);

    /**
     * Searches for messages containing specific text, case-insensitive.
     *
     * @param text The text to search for in message content
     * @return List of WhatsAppMessage entities containing the specified text
     * @throws org.springframework.dao.DataAccessException if there's an error accessing the database
     */
    List<WhatsAppMessage> findWhatsAppMessageByMessageContentContainingIgnoreCase(String text);

    /**
     * Retrieves message type statistics for a specific phone number.
     * Groups messages by type and returns count for each type.
     *
     * @param phoneNumber The phone number to analyze
     * @return List of Object arrays containing message type and count
     * @throws org.springframework.dao.DataAccessException if there's an error accessing the database
     */
    @Query("SELECT m.messageType, COUNT(m) " +
            "FROM WhatsAppMessage m " +
            "WHERE m.fromNumber = :phoneNumber " +
            "GROUP BY m.messageType")
    List<Object[]> findMessageTypeCountsByPhoneNumber(@Param("phoneNumber") String phoneNumber);
}