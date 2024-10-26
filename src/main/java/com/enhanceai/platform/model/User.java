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
    private Map<String, UserTextContent> textContents;
    private Map<String, UserFileContent> fileContents;

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", totalTextContents=" + totalTextContents +
                ", totalTextContents=" + totalFiles +
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
        return Objects.hash(id, name, totalTextContents, totalFiles);
    }

    public Map<String, UserFileContent> getFileContents() {
        return fileContents;
    }

    public void setFileContents(Map<String, UserFileContent> fileContents) {
        this.fileContents = fileContents;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTotalTextContents(int totalTextContents) {
        this.totalTextContents = totalTextContents;
    }


    public Map<String, UserTextContent> getTextContents() {
        return textContents;
    }

    public void setTextContents(Map<String, UserTextContent> textContents) {
        this.textContents = textContents;
    }
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTotalTextContents() {
        return totalTextContents;
    }

}
