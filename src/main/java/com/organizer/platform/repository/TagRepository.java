package com.organizer.platform.repository;

import com.organizer.platform.model.organizedDTO.Tag;
import com.organizer.platform.model.organizedDTO.WhatsAppMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);

    @Query("SELECT DISTINCT m FROM WhatsAppMessage m JOIN FETCH m.tags t WHERE t.name IN :tagNames")
    Set<WhatsAppMessage> findMessagesByTagNamesOptimized(@Param("tagNames") Set<String> tagNames);
}
