package com.enhanceai.platform.model;

import java.util.*;

public class UserFileContent {
    private Map<String, Integer> files = new HashMap<>();
    private List<String> contents = new ArrayList<>();

    public List<String> getContents() {
        return contents;
    }

    public void setContents(List<String> contents) {
        this.contents = contents;
    }

    public Map<String, Integer> getFiles() {
        return files;
    }

    public void setFiles(Map<String, Integer> files) {
        this.files = files;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserFileContent userFileContent = (UserFileContent) o;
        return Objects.equals(files, userFileContent.files) && contents.equals(userFileContent.contents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(files, contents);
    }

    @Override
    public String toString() {
        return "UserFileContent{" +
                "files=" + files +
                "contents" + contents +
                '}';
    }
}
