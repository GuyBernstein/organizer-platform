package com.organizer.platform.model.organizedDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryHierarchy {
    private String name;
    private String level;
    private Integer value;  // message count
    private Integer totalMessages;
    private Map<String, Integer> mimeDistribution; // Distribution of mime types
    private Map<String, Integer> purposeDistribution; // Distribution of purposes
    private Set<String> tags; // Aggregated tags
    private Set<String> nextSteps; // Aggregated next steps
    private List<CategoryHierarchy> children;
    private Date firstMessageDate;
    private Date lastMessageDate;

    public static List<CategoryHierarchy> buildCategoryHierarchy(Map<String, Map<String, List<MessageDTO>>> organizedMessages) {
        int totalMessages = organizedMessages.values().stream()
                .flatMap(subMap -> subMap.values().stream())
                .mapToInt(List::size)
                .sum();

        return organizedMessages.entrySet().stream()
                .map(categoryEntry -> {
                    String categoryName = categoryEntry.getKey();
                    Map<String, List<MessageDTO>> subCategories = categoryEntry.getValue();

                    // Aggregate category-level metrics
                    List<MessageDTO> allCategoryMessages = subCategories.values().stream()
                            .flatMap(List::stream)
                            .collect(Collectors.toList());

                    // Build category-level hierarchy

                    return CategoryHierarchy.builder()
                            .name(categoryName)
                            .level("category")
                            .value(allCategoryMessages.size())
                            .totalMessages(totalMessages)
                            .mimeDistribution(calculateMimeDistribution(allCategoryMessages))
                            .purposeDistribution(calculatePurposeDistribution(allCategoryMessages))
                            .tags(aggregateTags(allCategoryMessages))
                            .nextSteps(aggregateNextSteps(allCategoryMessages))
                            .firstMessageDate(findFirstMessageDate(allCategoryMessages))
                            .lastMessageDate(findLastMessageDate(allCategoryMessages))
                            .children(buildSubcategoryHierarchy(subCategories, totalMessages))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private static List<CategoryHierarchy> buildSubcategoryHierarchy(
            Map<String, List<MessageDTO>> subCategories,
            int totalMessages) {

        return subCategories.entrySet().stream()
                .map(subCategoryEntry -> {
                    String subCategoryName = subCategoryEntry.getKey();
                    List<MessageDTO> messages = subCategoryEntry.getValue();

                    return CategoryHierarchy.builder()
                            .name(subCategoryName)
                            .level("subcategory")
                            .value(messages.size())
                            .totalMessages(totalMessages)
                            .mimeDistribution(calculateMimeDistribution(messages))
                            .purposeDistribution(calculatePurposeDistribution(messages))
                            .tags(aggregateTags(messages))
                            .nextSteps(aggregateNextSteps(messages))
                            .firstMessageDate(findFirstMessageDate(messages))
                            .lastMessageDate(findLastMessageDate(messages))
                            .children(Collections.emptyList())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private static Map<String, Integer> calculateMimeDistribution(List<MessageDTO> messages) {
        return messages.stream()
                .collect(Collectors.groupingBy(
                        MessageDTO::getMime,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }

    private static Map<String, Integer> calculatePurposeDistribution(List<MessageDTO> messages) {
        return messages.stream()
                .collect(Collectors.groupingBy(
                        MessageDTO::getPurpose,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }

    private static Set<String> aggregateTags(List<MessageDTO> messages) {
        return messages.stream()
                .flatMap(msg -> msg.getTags().stream())
                .collect(Collectors.toSet());
    }

    private static Set<String> aggregateNextSteps(List<MessageDTO> messages) {
        return messages.stream()
                .flatMap(msg -> msg.getNextSteps().stream())
                .collect(Collectors.toSet());
    }

    private static Date findFirstMessageDate(List<MessageDTO> messages) {
        return messages.stream()
                .map(MessageDTO::getCreatedAt)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private static Date findLastMessageDate(List<MessageDTO> messages) {
        return messages.stream()
                .map(MessageDTO::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
