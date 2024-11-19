package com.organizer.platform.repository;

import com.organizer.platform.model.organizedDTO.WhatsAppMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {

    List<WhatsAppMessage> findByFromNumber(String fromNumber);
    @Query("SELECT DISTINCT m FROM WhatsAppMessage m JOIN m.tags t WHERE t.name IN :tagNames")
    List<WhatsAppMessage> findByTagNames(@Param("tagNames") Collection<String> tagNames);

    // a query to find messages with similar tags, ordered by relevance
    @Query("SELECT DISTINCT m, COUNT(t) as tagCount " +
           "FROM WhatsAppMessage m " +
           "JOIN m.tags t " +
           "WHERE t IN (SELECT t2 FROM WhatsAppMessage m2 JOIN m2.tags t2 WHERE m2.id = :messageId) " +
           "AND m.id != :messageId " +
           "GROUP BY m " +
           "ORDER BY tagCount DESC")
    List<WhatsAppMessage> findRelatedMessages(@Param("messageId") Long messageId);
    Optional<WhatsAppMessage> findWhatsAppMessageByMessageContentAndFromNumber(
            String messageContent, String fromNumber);
}
