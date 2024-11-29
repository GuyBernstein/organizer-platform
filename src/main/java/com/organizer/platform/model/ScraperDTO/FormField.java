package com.organizer.platform.model.ScraperDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormField {
    private String name;
    private String type;
    private String value;
    private boolean required;
}
