package com.organizer.platform.model.ScraperDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableInfo {
    private List<String> headers;
    private List<List<String>> rows;
    private String caption;
}
