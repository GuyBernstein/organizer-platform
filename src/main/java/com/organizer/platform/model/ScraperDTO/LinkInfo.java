package com.organizer.platform.model.ScraperDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkInfo {
    private String text;
    private String href;
    private String title;
    private boolean followable;
}
