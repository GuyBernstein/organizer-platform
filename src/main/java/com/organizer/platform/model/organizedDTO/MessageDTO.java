package com.organizer.platform.model.organizedDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {
    private Long id;
    private String messageContent;
    private String category;
    private String subCategory;
    private String type;
    private String purpose;
    private Set<String> tags;
    private Set<String> nextSteps;
}
