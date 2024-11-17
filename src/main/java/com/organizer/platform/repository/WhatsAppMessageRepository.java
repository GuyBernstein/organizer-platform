package com.organizer.platform.repository;

import com.organizer.platform.model.WhatsApp.WhatsAppMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, Long> {

    List<WhatsAppMessage> findByFromNumber(String fromNumber);
}
