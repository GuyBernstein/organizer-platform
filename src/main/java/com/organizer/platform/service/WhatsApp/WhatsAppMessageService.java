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

import javax.persistence.EntityNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

import static com.organizer.platform.model.organizedDTO.NextStep.NextStepBuilder.aNextStep;
import static com.organizer.platform.model.organizedDTO.Tag.TagBuilder.aTag;

@Service
public class WhatsAppMessageService {
    private final WhatsAppMessageRepository messageRepository;
    private final NextStepRepository nextStepRepository;
    private final TagRepository tagRepository;

    @Autowired
    public WhatsAppMessageService(WhatsAppMessageRepository messageRepository, NextStepRepository nextStepRepository,
                                  TagRepository tagRepository) {
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
        if (whatsAppMessage == null) {
            throw new IllegalArgumentException("WhatsAppMessage cannot be null");
        }

        // Process tags
        if (StringUtils.isNotBlank(tagsContent)) {
            Set<Tag> tagsSet = Arrays.stream(tagsContent.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(name -> tagRepository.findByName(name)
                            .orElseGet(() -> {
                                Tag newTag = new Tag();
                                newTag.setName(name);
                                return tagRepository.save(newTag);
                            }))
                    .collect(Collectors.toSet());

            whatsAppMessage.getTags().addAll(tagsSet);
            // Update the bidirectional relationship
            tagsSet.forEach(tag -> tag.getMessages().add(whatsAppMessage));
        }

        // Process next steps
        if (StringUtils.isNotBlank(nextStepsContent)) {
            Set<NextStep> nextStepsSet = Arrays.stream(nextStepsContent.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(name -> nextStepRepository.findByName(name)
                            .orElseGet(() -> {
                                NextStep newNextStep = new NextStep();
                                newNextStep.setName(name);
                                return nextStepRepository.save(newNextStep);
                            }))
                    .collect(Collectors.toSet());

            whatsAppMessage.getNextSteps().addAll(nextStepsSet);
            // Update the bidirectional relationship
            nextStepsSet.forEach(nextStep -> nextStep.getMessages().add(whatsAppMessage));
        }

        // Save the updated message
        messageRepository.save(whatsAppMessage);
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
}
