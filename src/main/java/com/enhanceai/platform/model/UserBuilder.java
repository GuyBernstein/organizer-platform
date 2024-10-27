package com.enhanceai.platform.model;

public final class UserBuilder {
    private String id;
    private String name;
    private int totalTextContents;
    private int totalFiles;
    private int totalAiContents;
    private UserContents textContents;
    private UserContents fileContents;
    private UserContents aiContents;

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

    public UserBuilder withTotalAiContents(int totalAiContents) {
        this.totalAiContents = totalAiContents;
        return this;
    }

    public UserBuilder withTextContents(UserContents textContents) {
        this.textContents = textContents;
        return this;
    }

    public UserBuilder withFileContents(UserContents fileContents) {
        this.fileContents = fileContents;
        return this;
    }

    public UserBuilder withAiContents(UserContents aiContents) {
        this.aiContents = aiContents;
        return this;
    }

    public User build() {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setTotalTextContents(totalTextContents);
        user.setTotalFiles(totalFiles);
        user.setTotalAiContents(totalAiContents);
        user.setTextContents(textContents);
        user.setFileContents(fileContents);
        user.setAiContents(aiContents);
        return user;
    }
}
