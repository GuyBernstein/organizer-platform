package com.organizer.platform.model.ScraperDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebPageContent {
    private String title;
    private List<String> mainContent;
    private NavigationInfo navigation;
    private List<HeadingInfo> headings;
    private List<LinkInfo> links;
    private Map<String, String> metadata;
}
