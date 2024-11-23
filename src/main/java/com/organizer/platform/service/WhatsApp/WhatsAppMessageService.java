package com.organizer.platform.service.WhatsApp;

import com.organizer.platform.model.organizedDTO.MessageDTO;
import com.organizer.platform.model.organizedDTO.NextStep;
import com.organizer.platform.model.organizedDTO.Tag;
import com.organizer.platform.model.organizedDTO.WhatsAppMessage;
import com.organizer.platform.repository.NextStepRepository;
import com.organizer.platform.repository.TagRepository;
import com.organizer.platform.repository.WhatsAppMessageRepository;
import com.organizer.platform.util.Dates;
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



    public Map<String, Map<String, List<MessageDTO>>> findMessageContentsByFromNumberGroupedByCategoryAndGroupedBySubCategory(String fromNumber) {
        List<WhatsAppMessage> messages = messageRepository.findByFromNumber(fromNumber).stream()
                .filter(message -> message.getMessageContent() != null && !message.getMessageContent().trim().isEmpty())
                .collect(Collectors.toList());

        return toOrganizedMessages(messages);
    }

    private Map<String, Map<String, List<MessageDTO>>> toOrganizedMessages(List<WhatsAppMessage> messages) {
        // Organize by category and subcategory hierarchy
        return messages.stream()
                .collect(Collectors.groupingBy(
                        message1 -> message1.getCategory() != null ? message1.getCategory() : "uncategorized",
                        Collectors.groupingBy(
                                message1 -> message1.getSubCategory() != null ? message1.getSubCategory() : "unsubcategorized",
                                Collectors.mapping(this::convertToMessageDTO, Collectors.toList())
                        )
                ));
    }

    // Method to find related messages with a minimum number of shared tags
    public Map<String, Map<String, List<MessageDTO>>> findRelatedMessagesWithMinimumSharedTags(WhatsAppMessage originalMessage, int minimumSharedTags) {

        Set<Tag> messageTags = originalMessage.getTags();

        if (messageTags.isEmpty()) {
            return Collections.emptyMap();
        }

        List<WhatsAppMessage> allRelatedMessages = messageRepository.findRelatedMessagesByTags(messageTags, originalMessage.getId());

        List<WhatsAppMessage> filteredMessages = getFilteredMessages(minimumSharedTags, allRelatedMessages, messageTags);

        return toOrganizedMessages(filteredMessages);
    }

    private static List<WhatsAppMessage> getFilteredMessages(int minimumSharedTags, List<WhatsAppMessage> allRelatedMessages, Set<Tag> messageTags) {
        // Filter messages based on minimum shared tags
        return allRelatedMessages.stream()
                .filter(message -> {
                    Set<Tag> intersection = new HashSet<>(message.getTags());
                    intersection.retainAll(messageTags);
                    return intersection.size() >= minimumSharedTags;
                })
                .collect(Collectors.toList());
    }

    private MessageDTO convertToMessageDTO(WhatsAppMessage message) {
        return MessageDTO.builder()
                .id(message.getId())
                .createdAt(message.getCreatedAt())
                .messageContent(message.getMessageContent())
                .category(message.getCategory())
                .subCategory(message.getSubCategory())
                .type(message.getType())
                .purpose(message.getPurpose())
                .tags(message.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toSet()))
                .nextSteps(message.getNextSteps().stream()
                        .map(NextStep::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

    public void addTagsAndNextSteps(WhatsAppMessage whatsAppMessage, String tagsContent, String nextStepsContent) {
        // Process tags (many-to-many)
        if (StringUtils.isNotBlank(tagsContent)) {
            addTags(whatsAppMessage, tagsContent);
        }

        // Process next steps
        if (StringUtils.isNotBlank(nextStepsContent)) {
            addNextSteps(whatsAppMessage, nextStepsContent);
        }
    }

    private void addNextSteps(WhatsAppMessage whatsAppMessage, String nextStepsContent) {
        Arrays.stream(nextStepsContent.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .forEach(name -> {
                    NextStep nextStep = nextStepRepository.findByName(name)
                            .orElseGet(() -> {
                                NextStep newNextStep = aNextStep()
                                        .name(name)
                                        .message(whatsAppMessage)
                                        .build();
                                        return nextStepRepository.save(newNextStep);
                                    });

                    // Set the message reference and save
                    whatsAppMessage.getNextSteps().add(nextStep);
                });
    }

    private void addTags(WhatsAppMessage whatsAppMessage, String tagsContent) {
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

    public Optional<WhatsAppMessage> findMessageById(Long messageId) {
        return messageRepository.findById(messageId);
    }

    @Transactional
    public MessageDTO partialUpdateMessage(Long messageId, MessageDTO updateRequest) {
        WhatsAppMessage message = findMessageById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));

        // Only update fields that are not null in the request
        if (updateRequest.getMessageContent() != null) {
            message.setMessageContent(updateRequest.getMessageContent());
        }

        if (updateRequest.getCategory() != null) {
            message.setCategory(updateRequest.getCategory());
        }

        if (updateRequest.getSubCategory() != null) {
            message.setSubCategory(updateRequest.getSubCategory());
        }

        if (updateRequest.getType() != null) {
            message.setType(updateRequest.getType());
        }

        if (updateRequest.getPurpose() != null) {
            message.setPurpose(updateRequest.getPurpose());
        }

        // Handle tags update
        if (updateRequest.getTags() != null) {
            // Clear existing tags
            message.getTags().clear();
            messageRepository.save(message); // Save to update the relationships

            // Convert Set<String> to comma-separated string and add new tags
            String tagsContent = String.join(",", updateRequest.getTags());
            if (!tagsContent.isEmpty()) {
                addTags(message, tagsContent);
            }
        }

        // Handle next steps update
        if (updateRequest.getNextSteps() != null) {
            // Clear existing next steps by removing message reference
            message.getNextSteps().forEach(nextStep -> {
                nextStep.setMessage(null);
                nextStepRepository.save(nextStep);
            });
            message.getNextSteps().clear();
            messageRepository.save(message); // Save to update the relationships

            // Convert Set<String> to comma-separated string and add new next steps
            String nextStepsContent = String.join(",", updateRequest.getNextSteps());
            if (!nextStepsContent.isEmpty()) {
                addNextSteps(message, nextStepsContent);
            }
        }

        // Refresh the message to get the updated relationships
        message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found after update"));


        // Convert to DTO and return
        return convertToMessageDTO(message);
    }
}
