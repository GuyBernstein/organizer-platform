package com.organizer.platform.model.ScraperDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextBlock {
    private String text;
    private String tag;
    private String cssClass;
    private String id;
    private int depth;
    private String parentTags;
    private boolean isVisible;
}
