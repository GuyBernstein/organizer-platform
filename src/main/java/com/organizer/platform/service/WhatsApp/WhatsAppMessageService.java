package com.organizer.platform.service.WhatsApp;

import com.organizer.platform.model.organizedDTO.*;
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

/**
 * Service class responsible for managing WhatsApp messages with advanced organization, categorization,
 * and relationship capabilities. This service provides comprehensive functionality for handling
 * WhatsApp message data within an organizational platform.
 *
 * Key Features:
 * 1. Message Organization
 *    - Groups messages by categories and subcategories
 *    - Handles message filtering and searching
 *    - Supports hierarchical organization of messages
 *
 * 2. Tag Management
 *    - Supports tagging of messages for better organization
 *    - Many-to-many relationship between messages and tags
 *    - Tag-based message filtering and retrieval
 *    - Smart tag cleanup to prevent orphaned tags
 *
 * 3. Next Steps Tracking
 *    - Associates action items or next steps with messages
 *    - Manages the lifecycle of next step entities
 *    - Supports updating and removing next steps
 *
 * 4. Advanced Search and Filtering
 *    - Content-based message search
 *    - Tag-based filtering
 *    - Related message discovery based on shared tags
 *    - Phone number specific queries
 *
 * 5. Data Management
 *    - Safe message creation and updates
 *    - Partial updates support
 *    - Cascade deletion handling
 *    - Database maintenance operations
 *
 * 6. Analytics Support
 *    - Message type statistics
 *    - Tag usage analytics
 *    - Phone number specific analytics
 *
 * Technical Details:
 * - Uses Spring's @Service and @Transactional annotations for proper transaction management
 * - Implements repository pattern for data access
 * - Handles null values and edge cases gracefully
 * - Provides optimized database operations for better performance
 * - Supports PostgreSQL-specific database operations
 *
 * Common Use Cases:
 * 1. Organizing messages by category/subcategory for better structure
 * 2. Finding related messages through shared tags
 * 3. Tracking action items associated with messages
 * 4. Analyzing message patterns and types
 * 5. Managing message metadata and relationships
 *
 * @see WhatsAppMessage
 * @see MessageDTO
 * @see Tag
 * @see NextStep
 * @see WhatsAppMessageRepository
 * @see TagRepository
 * @see NextStepRepository
 */
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

    /**
     * Retrieves and organizes WhatsApp messages for a specific phone number, grouping them by category and subcategory.
     *
     * @param fromNumber The phone number to fetch messages for
     * @return A nested Map structure where:
     *         - Outer key: Category (defaults to "uncategorized" if null)
     *         - Middle key: Subcategory (defaults to "unsubcategorized" if null)
     *         - Inner value: List of MessageDTO objects
     * @apiNote This method filters out:
     *         - Messages with null or empty content
     *         - Messages with null categories
     */
    public Map<String, Map<String, List<MessageDTO>>> findMessageContentsByFromNumberGroupedByCategoryAndGroupedBySubCategory(String fromNumber) {
        List<WhatsAppMessage> messages = messageRepository.findByFromNumber(fromNumber).stream()
                .filter(message -> message.getMessageContent() != null && !message.getMessageContent().trim().isEmpty())
                .filter(message -> message.getCategory() != null)  // Filter out null categories
                .collect(Collectors.toList());

        return toOrganizedMessages(messages);
    }

    /**
     * Retrieves all WhatsApp messages from a specific phone number.
     *
     * @param phone The phone number to fetch messages for
     * @return List of all WhatsAppMessage entities associated with the phone number
     * @apiNote This method does not apply any filtering on the messages
     */
    public List<WhatsAppMessage> findMessagesFromNumber(String phone){
        return messageRepository.findByFromNumber(phone);
    }

    /**
     * Transforms a list of WhatsAppMessage entities into a hierarchical structure organized by category and subcategory.
     *
     * @param messages List of WhatsAppMessage entities to organize
     * @return A nested Map structure where:
     *         - Outer key: Category (defaults to "uncategorized" if null)
     *         - Middle key: Subcategory (defaults to "unsubcategorized" if null)
     *         - Inner value: List of MessageDTO objects
     * @apiNote This method:
     *         - Filters out messages with null content
     *         - Converts WhatsAppMessage entities to MessageDTO objects
     *         - Handles null categories and subcategories with default values
     */
    private Map<String, Map<String, List<MessageDTO>>> toOrganizedMessages(List<WhatsAppMessage> messages) {
        return messages.stream()
                .filter(message -> message.getMessageContent() != null)  // Ensure message content exists
                .collect(Collectors.groupingBy(
                        message -> Optional.ofNullable(message.getCategory()).orElse("uncategorized"),
                        Collectors.groupingBy(
                                message -> Optional.ofNullable(message.getSubCategory()).orElse("unsubcategorized"),
                                Collectors.mapping(this::convertToMessageDTO, Collectors.toList())
                        )
                ));
    }

    /**
     * Filters an organized message structure to only include messages that exist in the filtered list.
     *
     * @param organizedMessages The original nested map structure of messages
     * @param filteredMessages List of MessageDTO objects to filter by
     * @return A new nested Map containing only the messages that exist in filteredMessages
     * @apiNote This method:
     *         - Preserves the original category/subcategory structure
     *         - Removes empty categories and subcategories
     *         - Uses object equality to match messages
     */
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

    /**
     * Searches for messages containing specific content within an organized message structure.
     *
     * @param content The text content to search for (case-insensitive)
     * @param organizedMessages The original nested map structure of messages to search within
     * @return A filtered nested Map containing only messages that match the search content
     * @apiNote This method:
     *         - Performs case-insensitive content matching
     *         - Returns an empty map if no matches are found
     *         - Preserves the category/subcategory structure for matching messages
     */
    public Map<String, Map<String, List<MessageDTO>>> getSearchedMessages(
            String content,
            Map<String, Map<String, List<MessageDTO>>> organizedMessages) {

        return messageRepository
                .findWhatsAppMessageByMessageContentContainingIgnoreCase(content)
                .stream()
                .map(this::convertToMessageDTO)
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        messages -> messages.isEmpty()
                                ? Collections.emptyMap()
                                : filterOrganizedMessages(organizedMessages, messages)
                ));
    }

    /**
     * Retrieves WhatsApp messages that are associated with any of the specified tags.
     * This method uses an optimized query to fetch messages with their tags in a single operation.
     *
     * @param tagNames A set of tag names to search for
     * @return A set of WhatsAppMessage objects that have at least one of the specified tags
     */
    public Set<WhatsAppMessage> getMessagesByTags(Set<String> tagNames) {
        return tagRepository.findMessagesByTagNamesOptimized(tagNames);
    }

    /**
     * Filters messages based on tag names and phone number, organizing them into a hierarchical structure.
     * This method combines tag-based filtering with category-based organization.
     *
     * @param tagNames Set of tag names to filter messages by
     * @param phoneNumber Phone number to filter messages for
     * @return A nested map structure organizing filtered messages by category and subcategory:
     *         Map<Category, Map<SubCategory, List<MessageDTO>>>
     */
    public Map<String, Map<String, List<MessageDTO>>> getFilteredMessages(Set<String> tagNames, String phoneNumber) {
        // Retrieve all messages that contain any of the specified tags
        Set<WhatsAppMessage> messages = getMessagesByTags(tagNames);

        // Convert the filtered messages to DTOs
        List<MessageDTO> filteredMessages = messages.stream()
                .map(this::convertToMessageDTO)
                .collect(Collectors.toList());

        // Get all messages for the phone number, organized by category and subcategory
        Map<String, Map<String, List<MessageDTO>>> organizedMessages =
                findMessageContentsByFromNumberGroupedByCategoryAndGroupedBySubCategory(phoneNumber);

        // Filter the organized messages to only include those that match the tag filter
        return filterOrganizedMessages(organizedMessages, filteredMessages);
    }

    /**
     * Finds messages related to a given message based on shared tags, requiring a minimum number of common tags.
     * Messages are considered related if they share at least the specified number of tags with the original message.
     *
     * @param originalMessage The source message to find related messages for
     * @param minimumSharedTags The minimum number of tags that must be shared between messages
     * @return A nested map structure organizing related messages by category and subcategory:
     *         Map<Category, Map<SubCategory, List<MessageDTO>>>
     */
    public Map<String, Map<String, List<MessageDTO>>> findRelatedMessagesWithMinimumSharedTags(
            WhatsAppMessage originalMessage,
            int minimumSharedTags) {

        Set<Tag> messageTags = originalMessage.getTags();

        // Return empty map if original message has no tags
        if (messageTags.isEmpty()) {
            return Collections.emptyMap();
        }

        // Find all messages that share any tags with the original message
        List<WhatsAppMessage> allRelatedMessages = messageRepository.findRelatedMessagesByTags(
                messageTags,
                originalMessage.getId()
        );

        // Filter messages to ensure they meet the minimum shared tags requirement
        List<WhatsAppMessage> filteredMessages = getFilteredMessages(
                minimumSharedTags,
                allRelatedMessages,
                messageTags
        );

        // Organize the filtered messages by category and subcategory
        return toOrganizedMessages(filteredMessages);
    }

    /**
     * Filters a list of messages based on a minimum number of shared tags with a reference set of tags.
     * This is a helper method used for finding related messages.
     *
     * @param minimumSharedTags The minimum number of tags that must be shared
     * @param allRelatedMessages List of potentially related messages to filter
     * @param messageTags The reference set of tags to compare against
     * @return List of messages that share at least the minimum number of tags
     */
    private static List<WhatsAppMessage> getFilteredMessages(
            int minimumSharedTags,
            List<WhatsAppMessage> allRelatedMessages,
            Set<Tag> messageTags) {

        return allRelatedMessages.stream()
                .filter(message -> {
                    Set<Tag> intersection = new HashSet<>(message.getTags());
                    intersection.retainAll(messageTags);
                    return intersection.size() >= minimumSharedTags;
                })
                .collect(Collectors.toList());
    }

    /**
     * Converts a WhatsAppMessage entity to a MessageDTO, handling null values and empty collections.
     * This method provides a safe conversion with default values for null fields.
     *
     * @param message The WhatsAppMessage entity to convert
     * @return A MessageDTO containing the message data with safe null handling
     */
    public MessageDTO convertToMessageDTO(WhatsAppMessage message) {
        return MessageDTO.builder()
                .id(message.getId())
                .createdAt(message.getCreatedAt())
                // Provide empty string for null message content
                .messageContent(Optional.ofNullable(message.getMessageContent()).orElse(""))
                // Default category to "uncategorized" if null
                .category(Optional.ofNullable(message.getCategory()).orElse("uncategorized"))
                // Default subcategory to "unsubcategorized" if null
                .subCategory(Optional.ofNullable(message.getSubCategory()).orElse("unsubcategorized"))
                // Provide empty string for null type
                .type(Optional.ofNullable(message.getType()).orElse(""))
                // Provide empty string for null purpose
                .purpose(Optional.ofNullable(message.getPurpose()).orElse(""))
                // Convert tags to set of tag names, handling null tags
                .tags(Optional.ofNullable(message.getTags())
                        .orElse(Collections.emptySet())
                        .stream()
                        .filter(Objects::nonNull)
                        .map(Tag::getName)
                        .collect(Collectors.toSet()))
                // Convert next steps to set of step names, handling null steps
                .nextSteps(Optional.ofNullable(message.getNextSteps())
                        .orElse(Collections.emptySet())
                        .stream()
                        .filter(Objects::nonNull)
                        .map(NextStep::getName)
                        .collect(Collectors.toSet()))
                // Provide empty string for null message type
                .mime(Optional.ofNullable(message.getMessageType()).orElse(""))
                .build();
    }

    /**
     * Associates tags and next steps with a WhatsApp message by processing comma-separated strings.
     * This method handles both the creation of new tags/next steps and linking of existing ones.
     *
     * @param whatsAppMessage The WhatsApp message to update
     * @param tagsContent Comma-separated string of tag names to associate with the message
     * @param nextStepsContent Comma-separated string of next step names to associate with the message
     */
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

    /**
     * Processes a comma-separated string of next step names and associates them with a WhatsApp message.
     * For each next step name:
     * - If it exists, retrieves it from the repository
     * - If it doesn't exist, creates a new NextStep entity
     * - Associates the next step with the message
     *
     * @param whatsAppMessage The WhatsApp message to associate next steps with
     * @param nextStepsContent Comma-separated string of next step names
     */
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

    /**
     * Processes a comma-separated string of tag names and associates them with a WhatsApp message.
     * For each tag name:
     * - If it exists, retrieves it from the repository
     * - If it doesn't exist, creates a new Tag entity
     * - Associates the tag with the message using a many-to-many relationship
     *
     * @param whatsAppMessage The WhatsApp message to associate tags with
     * @param tagsContent Comma-separated string of tag names
     */
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

    /**
     * Saves a WhatsApp message to the repository after validation.
     *
     * @param whatsAppMessage The WhatsApp message to save
     * @return The saved WhatsApp message with updated database information
     * @throws IllegalArgumentException if the provided message is null
     */
    public WhatsAppMessage save(WhatsAppMessage whatsAppMessage) {
        if (whatsAppMessage == null) {
            throw new IllegalArgumentException("WhatsAppMessage cannot be null");
        }
        return messageRepository.save(whatsAppMessage);
    }

    /**
     * Cleans the database by truncating all related tables while properly handling PostgreSQL constraints.
     * This method:
     * 1. Temporarily disables all triggers
     * 2. Truncates join tables first (message_next_steps, message_tags)
     * 3. Truncates main tables (next_steps, whatsapp_message, tags)
     * 4. Re-enables all triggers
     *
     * Note: This operation is irreversible and should be used with caution.
     * The method uses a transaction to ensure data consistency.
     */
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

    /**
     * Retrieves a WhatsApp message by its unique identifier.
     *
     * @param messageId The unique identifier of the message to retrieve
     * @return Optional<WhatsAppMessage> containing the message if found, empty Optional otherwise
     */
    public Optional<WhatsAppMessage> findMessageById(Long messageId) {
        return messageRepository.findById(messageId);
    }

    /**
     * Partially updates a WhatsApp message with new data from a MessageDTO.
     * Only non-null fields in the updateRequest will be applied to the message.
     * Handles both basic fields and relationship updates (tags and next steps).
     *
     * @param message The existing WhatsApp message to update
     * @param updateRequest DTO containing the fields to update
     * @return MessageDTO representing the updated message
     * @throws javax.persistence.EntityNotFoundException if the message doesn't exist
     */
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

    /**
     * Replaces all next steps associated with a message with a new set of next steps.
     * First removes all existing next steps, then adds the new ones.
     *
     * @param message The WhatsApp message whose next steps should be replaced
     * @param nextSteps Set of next step names to associate with the message
     */
    private void replaceNextSteps(WhatsAppMessage message, Set<String> nextSteps) {
        // Remove all existing next steps
        deleteNextSteps(message);

        // Add new next steps if any are provided
        String nextStepsContent = String.join(",", nextSteps);
        if (!nextStepsContent.isEmpty()) {
            addNextSteps(message, nextStepsContent);
        }
    }

    /**
     * Replaces all tags associated with a message with a new set of tags.
     * First removes all existing tags, then adds the new ones.
     * If a tag no longer has any messages associated with it, it will be deleted.
     *
     * @param message The WhatsApp message whose tags should be replaced
     * @param tags Set of tag names to associate with the message
     */
    private void replaceTags(WhatsAppMessage message, Set<String> tags) {
        // Remove all existing tags
        deleteTags(message);

        // Add new tags if any are provided
        String tagsContent = String.join(",", tags);
        if (!tagsContent.isEmpty()) {
            addTags(message, tagsContent);
        }
    }

    /**
     * Removes all next steps associated with a message.
     * This method handles both sides of the relationship:
     * 1. Deletes the next step entities from the database
     * 2. Clears the next steps collection in the message entity
     *
     * @param message The WhatsApp message whose next steps should be deleted
     */
    public void deleteNextSteps(WhatsAppMessage message) {
        // Delete all next step entities associated with this message
        nextStepRepository.deleteAll(message.getNextSteps());
        // Clear the next steps collection in the message entity
        message.getNextSteps().clear();
    }

    /**
     * Removes all tag associations from a WhatsApp message and deletes orphaned tags.
     * This method handles the many-to-many relationship cleanup between messages and tags.
     *
     * @param message The WhatsApp message whose tags should be removed
     *
     * Process:
     * 1. For each tag associated with the message:
     *    - Removes the message reference from the tag's message collection
     *    - If the tag has no more associated messages, deletes the tag entirely
     * 2. Clears all tag references from the message
     */
    public void deleteTags(WhatsAppMessage message) {
        message.getTags().forEach((tag -> { // disconnect the relation from the tags to this message
            tag.getMessages().remove(message);
            if(tag.getMessages().isEmpty()) // if removed the last message
                tagRepository.delete(tag);
        }));
        message.getTags().clear(); // disconnect the relation from the other side
    }

    /**
     * Deletes a WhatsApp message and all its associated relationships.
     * This method ensures proper cleanup of all related entities to prevent orphaned records.
     *
     * @param messageId The ID of the message to delete
     *
     * Process:
     * 1. Retrieves the message by ID
     * 2. If message exists:
     *    - Removes all next step associations
     *    - Removes all tag associations
     *    - Deletes the message itself
     */
    @Transactional
    public void deleteMessage(Long messageId) {
        var whatsAppMessage = findMessageById(messageId);
        if(whatsAppMessage.isEmpty())
            return;
        deleteNextSteps(whatsAppMessage.get());
        deleteTags(whatsAppMessage.get());
        messageRepository.deleteById(messageId);
    }

    /**
     * Retrieves all unique tag names associated with messages from a specific phone number.
     * Uses an optimized query to fetch only the tag names without loading full tag objects.
     *
     * @param phoneNumber The phone number to search for
     * @return A Set of tag names associated with messages from the given phone number
     *
     * Note: This method uses a custom JPQL query defined in TagRepository to optimize performance
     * by fetching only the required tag names instead of entire tag objects.
     */
    public Set<String> getAllTagsByPhoneNumber(String phoneNumber) {
        return tagRepository.findTagNamesByPhoneNumber(phoneNumber);
    }

    /**
     * Retrieves message type statistics for a specific phone number.
     * Groups messages by their type (e.g., text, image, video) and provides counts with appropriate icons.
     *
     * @param phoneNumber The phone number to analyze
     * @return List of MessageTypeCount objects containing type, icon, and count information
     *
     * Process:
     * 1. Executes optimized query to get message type counts
     * 2. Transforms raw query results into MessageTypeCount objects
     * 3. Assigns appropriate icons based on message type
     *
     * Note: Uses custom query in WhatsAppMessageRepository for efficient grouping and counting
     */
    public List<MessageTypeCount> getMessageTypesByPhoneNumber(String phoneNumber) {
        // Get counts grouped by message type
        List<Object[]> results = messageRepository.findMessageTypeCountsByPhoneNumber(phoneNumber);

        // Convert to MessageTypeCount objects
        return results.stream()
                .map(result -> new MessageTypeCount(
                        (String) result[0],
                        MessageTypeCount.getIconForType((String) result[0]),
                        (Long) result[1]
                ))
                .collect(Collectors.toList());
    }
}
