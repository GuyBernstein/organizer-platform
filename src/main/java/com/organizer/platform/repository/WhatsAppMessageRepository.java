package com.organizer.platform.repository;

import com.organizer.platform.model.organizedDTO.Tag;
import com.organizer.platform.model.organizedDTO.WhatsAppMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {

    List<WhatsAppMessage> findByFromNumber(String fromNumber);
    @Query("SELECT DISTINCT m " +
            "FROM WhatsAppMessage m JOIN m.tags t " +
            "WHERE t IN :tags AND m.id != :messageId")
    List<WhatsAppMessage> findRelatedMessagesByTags(@Param("tags") Set<Tag> tags, @Param("messageId") Long messageId);
    List<WhatsAppMessage> findWhatsAppMessageByMessageContentContainingIgnoreCase(String text);
    @Query("SELECT m.messageType, COUNT(m) " +
            "FROM WhatsAppMessage m " +
            "WHERE m.fromNumber = :phoneNumber " +
            "GROUP BY m.messageType")
    List<Object[]> findMessageTypeCountsByPhoneNumber(@Param("phoneNumber") String phoneNumber);

}
