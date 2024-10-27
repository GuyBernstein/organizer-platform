package com.enhanceai.platform.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class UserContents {
    private Map<String, Map<String, Content>> content = new HashMap<>();

    public Map<String, Map<String, Content>> getContent() {
        return content;
    }

    public void setContent(Map<String, Map<String, Content>> content) {
        this.content = content;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserContents userContents = (UserContents) o;
        return Objects.equals(content, userContents.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }

    @Override
    public String toString() {
        return "UserContents{" +
                "count=" + content +
                '}';
    }

    public void addContent(String type, String monthKey, String contentId) {
        content.computeIfAbsent(type, k -> new HashMap<>());
        Map<String, Content> monthContent = content.get(type);

        monthContent.computeIfAbsent(monthKey, k -> new Content());
        Content monthData = monthContent.get(monthKey);

        monthData.incrementCount();
        monthData.addContentId(contentId);
    }

}
