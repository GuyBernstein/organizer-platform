package com.enhanceai.platform.model;

import java.util.Map;

public final class UserBuilder {
    private String id;
    private String name;
    private int totalTextContents;
    private int totalFiles;
    private Map<String, UserTextContent> textContents;
    private Map<String, UserFileContent> fileContents;

    private UserBuilder() {
    }

    public static UserBuilder anUser() {
        return new UserBuilder();
    }

    public UserBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public UserBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public UserBuilder withTotalTextContents(int totalTextContents) {
        this.totalTextContents = totalTextContents;
        return this;
    }

    public UserBuilder withTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
        return this;
    }

    public UserBuilder withTextContents(Map<String, UserTextContent> textContents) {
        this.textContents = textContents;
        return this;
    }

    public UserBuilder withFileContents(Map<String, UserFileContent> fileContents) {
        this.fileContents = fileContents;
        return this;
    }

    public User build() {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setTotalTextContents(totalTextContents);
        user.setTotalFiles(totalFiles);
        user.setTextContents(textContents);
        user.setFileContents(fileContents);
        return user;
    }
}
