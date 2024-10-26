package com.enhanceai.platform.model;

import java.util.*;

public class UserTextContent {
    private Map<String, Integer> texts = new HashMap<>();
    private List<String> contents = new ArrayList<>();

    public List<String> getContents() {
        return contents;
    }

    public void setContents(List<String> contents) {
        this.contents = contents;
    }

    public Map<String, Integer> getTexts() {
        return texts;
    }

    public void setTexts(Map<String, Integer> texts) {
        this.texts = texts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserTextContent userTextContent = (UserTextContent) o;
        return Objects.equals(texts, userTextContent.texts) && contents.equals(userTextContent.contents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(texts, contents);
    }

    @Override
    public String toString() {
        return "UserTextContent{" +
                "texts=" + texts +
                "content=" + contents +
                '}';
    }

}
