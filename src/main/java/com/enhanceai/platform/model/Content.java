package com.enhanceai.platform.model;

import com.enhanceai.platform.util.Dates;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Content {
    private int count;
    private Date lastUpdated;
    private List<String> contentIds;

    public Content() {
        this.count = 0;
        this.lastUpdated = Dates.nowUTC();
        this.contentIds = new ArrayList<>();
    }

    // Getters and setters
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public List<String> getContentIds() {
        return contentIds;
    }

    public void setContentIds(List<String> contentIds) {
        this.contentIds = contentIds;
    }

    public void incrementCount() {
        this.count++;
        this.lastUpdated = new Date();
    }

    public void addContentId(String contentId) {
        if (this.contentIds == null) {
            this.contentIds = new ArrayList<>();
        }
        this.contentIds.add(contentId);
    }
}

