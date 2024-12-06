package com.organizer.platform.service.WhatsApp;

import com.organizer.platform.model.organizedDTO.MessageDTO;
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

    public Map<String, Map<String, List<MessageDTO>>> filterOrganizedMessages(
            Map<String, Map<String, List<MessageDTO>>> organizedMessages,
            List<MessageDTO> filteredMessages) {

        Map<String, Map<String, List<MessageDTO>>> result = new HashMap<>();

        for (var categoryEntry : organizedMessages.entrySet()) {
            Map<String, List<MessageDTO>> subCategoryMap = new HashMap<>();

            for (var subCategoryEntry : categoryEntry.getValue().entrySet()) {
                List<MessageDTO> filteredList = subCategoryEntry.getValue().stream()
                        .filter(filteredMessages::contains)
                        .collect(Collectors.toList());

                if (!filteredList.isEmpty()) {
                    subCategoryMap.put(subCategoryEntry.getKey(), filteredList);
                }
            }

            if (!subCategoryMap.isEmpty()) {
                result.put(categoryEntry.getKey(), subCategoryMap);
            }
        }

        return result;
    }

    public Set<WhatsAppMessage> getMessagesByTags(Set<String> tagNames) {
        return tagRepository.findMessagesByTagNamesOptimized(tagNames);
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

    public MessageDTO convertToMessageDTO(WhatsAppMessage message) {
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
    public MessageDTO partialUpdateMessage(WhatsAppMessage message, MessageDTO updateRequest) {
        // Update fields only if they're not null in the request
        Optional.ofNullable(updateRequest.getMessageContent())
                .ifPresent(message::setMessageContent);
        Optional.ofNullable(updateRequest.getCategory())
                .ifPresent(message::setCategory);
        Optional.ofNullable(updateRequest.getSubCategory())
                .ifPresent(message::setSubCategory);
        Optional.ofNullable(updateRequest.getType())
                .ifPresent(message::setType);
        Optional.ofNullable(updateRequest.getPurpose())
                .ifPresent(message::setPurpose);
        Optional.ofNullable(updateRequest.getTags())
                .ifPresent(tags -> replaceTags(message, tags));
        Optional.ofNullable(updateRequest.getNextSteps())
                .ifPresent(nextSteps -> replaceNextSteps(message, nextSteps));

        // Save and refresh to get updated relationships
        return convertToMessageDTO(messageRepository.save(message));
    }

    private void replaceNextSteps(WhatsAppMessage message, Set<String> nextSteps) {
        deleteNextSteps(message);

        String nextStepsContent = String.join(",", nextSteps);
        if (!nextStepsContent.isEmpty()) {
            addNextSteps(message, nextStepsContent);
        }
    }

    private void replaceTags(WhatsAppMessage message, Set<String> tags) {
        deleteTags(message);

        String tagsContent = String.join(",", tags);
        if (!tagsContent.isEmpty()) {
            addTags(message, tagsContent);
        }
    }

    public void deleteNextSteps(WhatsAppMessage message) {
        nextStepRepository.deleteAll(message.getNextSteps()); // disconnect the relation from the next steps to this message
        message.getNextSteps().clear(); // disconnect the relation from the other side
    }

    public void deleteTags(WhatsAppMessage message) {
        message.getTags().forEach((tag -> { // disconnect the relation from the tags to this message
            tag.getMessages().remove(message);
            if(tag.getMessages().isEmpty()) // if removed the last message
                tagRepository.delete(tag);
        }));
        message.getTags().clear(); // disconnect the relation from the other side
    }

    @Transactional
    public void deleteMessage(Long messageId) {
        var whatsAppMessage = findMessageById(messageId);
        if(whatsAppMessage.isEmpty())
            return;
        deleteNextSteps(whatsAppMessage.get());
        deleteTags(whatsAppMessage.get());
        messageRepository.deleteById(messageId);
    }

    public Set<String> getAllTags(){
        List<Tag> tags = tagRepository.findAll();
        return tags.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());
    }
}
