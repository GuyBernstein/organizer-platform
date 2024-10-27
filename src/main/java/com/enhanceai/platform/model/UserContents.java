package com.enhanceai.platform.model;

import java.util.*;

public class UserContents {
    private Map<String, Integer> contents = new HashMap<>();

    public Map<String, Integer> getContents() {
        return contents;
    }

    public void setContents(Map<String, Integer> contents) {
        this.contents = contents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserContents userContents = (UserContents) o;
        return Objects.equals(contents, userContents.contents);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contents);
    }

    @Override
    public String toString() {
        return "UserContents{" +
                "content=" + contents +
                '}';
    }

}
