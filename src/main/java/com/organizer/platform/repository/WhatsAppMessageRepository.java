package com.organizer.platform.repository;

import com.organizer.platform.model.WhatsAppMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {

    List<WhatsAppMessage> findByFromNumber(String fromNumber);
}
