package com.organizer.platform.service;

import com.organizer.platform.model.WhatsAppMessage;
import com.organizer.platform.repository.WhatsAppMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WhatsAppMessageService {
    private final WhatsAppMessageRepository repository;

    @Autowired
    public WhatsAppMessageService(WhatsAppMessageRepository repository) {
        this.repository = repository;
    }

    public Map<String, List<String>> findMessageContentsByFromNumberGroupedByCategory(String fromNumber) {
        return repository.findByFromNumber(fromNumber).stream()
                .filter(message -> message.getMessageContent() != null && !message.getMessageContent().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        message -> message.getCategory() != null ? message.getCategory() : "uncategorized",
                        Collectors.mapping(WhatsAppMessage::getMessageContent, Collectors.toList())
                ));
    }

    public WhatsAppMessage save(WhatsAppMessage whatsAppMessage) {
        if (whatsAppMessage == null) {
            throw new IllegalArgumentException("WhatsAppMessage cannot be null");
        }
        return repository.save(whatsAppMessage);
    }

    public void delete(WhatsAppMessage whatsAppMessage) {
        repository.delete(whatsAppMessage);
    }

    public void deleteAll() {
        repository.deleteAll();
    }
}
