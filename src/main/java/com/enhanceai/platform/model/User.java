package com.enhanceai.platform.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;
import java.util.Objects;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;
    private int totalTextContents;
    private int totalFiles;
    private int totalAiContents;
    private Map<String, UserContents> textContents;
    private Map<String, UserContents> fileContents;
    private Map<String, UserContents> aiContents;

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

    public Map<String, UserContents> getTextContents() {
        return textContents;
    }

    public void setTextContents(Map<String, UserContents> textContents) {
        this.textContents = textContents;
    }

    public Map<String, UserContents> getFileContents() {
        return fileContents;
    }

    public void setFileContents(Map<String, UserContents> fileContents) {
        this.fileContents = fileContents;
    }

    public Map<String, UserContents> getAiContents() {
        return aiContents;
    }

    public void setAiContents(Map<String, UserContents> aiContents) {
        this.aiContents = aiContents;
    }
}
