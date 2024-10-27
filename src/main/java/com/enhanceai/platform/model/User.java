package com.enhanceai.platform.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;
import java.util.Objects;

import static com.enhanceai.platform.util.Dates.getCurMonth;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;
    private int totalTextContents;
    private int totalFiles;
    private int totalAiContents;
    private UserContents textContents;
    private UserContents fileContents;
    private UserContents aiContents;

    public void addContent(String contentType, String type, String contentId) {
        String monthKey = getCurMonth();

        switch (contentType) {
            case "text":
                if (textContents == null) textContents = new UserContents();
                textContents.addContent(type, monthKey, contentId);
                totalTextContents++;
                break;
            case "file":
                if (fileContents == null) fileContents = new UserContents();
                fileContents.addContent(type, monthKey, contentId);
                totalFiles++;
                break;
            case "ai":
                if (aiContents == null) aiContents = new UserContents();
                aiContents.addContent(type, monthKey, contentId);
                totalAiContents++;
                break;
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", totalTextContents=" + totalTextContents +
                ", totalFiles=" + totalFiles +
                ", totalAiContents" + totalAiContents +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return totalFiles == user.totalFiles && totalTextContents == user.totalTextContents &&
                Objects.equals(id, user.id) && Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, totalTextContents, totalFiles, totalAiContents);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getTotalTextContents() {
        return totalTextContents;
    }

    public void setTotalTextContents(int totalTextContents) {
        this.totalTextContents = totalTextContents;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getTotalAiContents() {
        return totalAiContents;
    }

    public void setTotalAiContents(int totalAiContents) {
        this.totalAiContents = totalAiContents;
    }

    public UserContents getTextContents() {
        return textContents;
    }

    public void setTextContents(UserContents textContents) {
        this.textContents = textContents;
    }

    public UserContents getFileContents() {
        return fileContents;
    }

    public void setFileContents(UserContents fileContents) {
        this.fileContents = fileContents;
    }

    public UserContents getAiContents() {
        return aiContents;
    }

    public void setAiContents(UserContents aiContents) {
        this.aiContents = aiContents;
    }
}
