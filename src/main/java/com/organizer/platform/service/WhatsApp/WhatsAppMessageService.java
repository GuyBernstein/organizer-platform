package com.organizer.platform.service.WhatsApp;

import com.organizer.platform.model.organizedDTO.NextStep;
import com.organizer.platform.model.organizedDTO.Tag;
import com.organizer.platform.model.organizedDTO.WhatsAppMessage;
import com.organizer.platform.repository.NextStepRepository;
import com.organizer.platform.repository.TagRepository;
import com.organizer.platform.repository.WhatsAppMessageRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

import static com.organizer.platform.model.organizedDTO.NextStep.NextStepBuilder.aNextStep;
import static com.organizer.platform.model.organizedDTO.Tag.TagBuilder.aTag;

@Service
public class WhatsAppMessageService {
    private final EntityManager entityManager;
    private final WhatsAppMessageRepository messageRepository;
    private final NextStepRepository nextStepRepository;
    private final TagRepository tagRepository;

    @Autowired
    public WhatsAppMessageService(EntityManager entityManager, WhatsAppMessageRepository messageRepository, NextStepRepository nextStepRepository,
                                  TagRepository tagRepository) {
        this.entityManager = entityManager;
        this.messageRepository = messageRepository;
        this.nextStepRepository = nextStepRepository;
        this.tagRepository = tagRepository;
    }

    public Map<String, List<String>> findMessageContentsByFromNumberGroupedByCategory(String fromNumber) {
        return messageRepository.findByFromNumber(fromNumber).stream()
                .filter(message -> message.getMessageContent() != null && !message.getMessageContent().trim().isEmpty())
                .collect(Collectors.groupingBy(
                        message -> message.getCategory() != null ? message.getCategory() : "uncategorized",
                        Collectors.mapping(WhatsAppMessage::getMessageContent, Collectors.toList())
                ));
    }

    public Map<String, List<WhatsAppMessage>> findMessagesByTagsGrouped(Set<String> tagNames) {
        List<WhatsAppMessage> messages = findMessagesByTags(tagNames);
        return messages.stream()
                .collect(Collectors.groupingBy(
                        message -> message.getTags().stream()
                                .map(Tag::getName)
                                .collect(Collectors.joining(", "))
                ));
    }

    public void addTagsAndNextSteps(WhatsAppMessage whatsAppMessage, String tagsContent, String nextStepsContent) {
        // Process tags (many-to-many)
        if (StringUtils.isNotBlank(tagsContent)) {
            Arrays.stream(tagsContent.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .forEach(name -> {
                        Tag tag = tagRepository.findByName(name)
                                .orElseGet(() -> {
                                    Tag newTag = aTag()
                                            .name(name)
                                            .build();
                                    return tagRepository.save(newTag);
                                });
                        whatsAppMessage.getTags().add(tag);
                    });

            // Save the message again to update the tags relationship
            messageRepository.save(whatsAppMessage);
        }

        // Process next steps
        if (StringUtils.isNotBlank(nextStepsContent)) {
            Arrays.stream(nextStepsContent.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .forEach(name -> {
                        NextStep nextStep = nextStepRepository.findByName(name)
                                .orElseGet(() -> aNextStep()
                                        .name(name)
                                        .build());

                        // Set the message reference and save
                        nextStep.setMessage(whatsAppMessage);
                        nextStepRepository.save(nextStep);
                    });
        }
    }

    public List<WhatsAppMessage> findRelatedMessages(Long messageId) {
        return messageRepository.findRelatedMessages(messageId);
    }

    public List<WhatsAppMessage> findMessagesByTags(Set<String> tagNames) {
        return messageRepository.findByTagNames(tagNames);
    }

    public WhatsAppMessage save(WhatsAppMessage whatsAppMessage) {
        if (whatsAppMessage == null) {
            throw new IllegalArgumentException("WhatsAppMessage cannot be null");
        }
        return messageRepository.save(whatsAppMessage);
    }

    public void delete(WhatsAppMessage whatsAppMessage) {
        messageRepository.delete(whatsAppMessage);
    }

    public void deleteAllMessages() {
        messageRepository.deleteAll();
    }
    public void deleteAllTags() {
        tagRepository.deleteAll();
    }

    public void deleteAllNextSteps() { nextStepRepository.deleteAll();
    }

    @Transactional
    public void cleanDatabase() {
        // First, clear the many-to-many relationships
        List<WhatsAppMessage> messages = messageRepository.findAll();
        for (WhatsAppMessage message : messages) {
            message.getTags().clear();
        }
        messageRepository.saveAll(messages);

        // Clear all next steps (child entities)
        nextStepRepository.deleteAll();

        // Delete all messages
        messageRepository.deleteAll();

        // Finally, delete all tags
        tagRepository.deleteAll();
    }

    @Transactional
    public void cleanDatabasePostgres() {
        // Disable trigger temporarily
        entityManager.createNativeQuery("ALTER TABLE message_next_steps DISABLE TRIGGER ALL").executeUpdate();
        entityManager.createNativeQuery("ALTER TABLE message_tags DISABLE TRIGGER ALL").executeUpdate();
        entityManager.createNativeQuery("ALTER TABLE next_steps DISABLE TRIGGER ALL").executeUpdate();
        entityManager.createNativeQuery("ALTER TABLE whatsapp_message DISABLE TRIGGER ALL").executeUpdate();
        entityManager.createNativeQuery("ALTER TABLE tags DISABLE TRIGGER ALL").executeUpdate();

        try {
            // Clean all join tables first
            entityManager.createNativeQuery("TRUNCATE TABLE message_next_steps CASCADE").executeUpdate();
            entityManager.createNativeQuery("TRUNCATE TABLE message_tags CASCADE").executeUpdate();

            // Clean main tables
            entityManager.createNativeQuery("TRUNCATE TABLE next_steps CASCADE").executeUpdate();
            entityManager.createNativeQuery("TRUNCATE TABLE whatsapp_message CASCADE").executeUpdate();
            entityManager.createNativeQuery("TRUNCATE TABLE tags CASCADE").executeUpdate();

        } finally {
            // Re-enable triggers
            entityManager.createNativeQuery("ALTER TABLE message_next_steps ENABLE TRIGGER ALL").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE message_tags ENABLE TRIGGER ALL").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE next_steps ENABLE TRIGGER ALL").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE whatsapp_message ENABLE TRIGGER ALL").executeUpdate();
            entityManager.createNativeQuery("ALTER TABLE tags ENABLE TRIGGER ALL").executeUpdate();
        }
    }



}
