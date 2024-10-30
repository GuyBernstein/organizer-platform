package com.enhanceai.platform.model;


import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Objects;

@Table("usercontent")
public class UserContent {
    @PrimaryKey
    private UserContentKey userContentKey;

    private String contentId;  
    private String contentType;  // "TEXT", "FILE", "AI_RESPONSE"
    private String content;  // The actual text content or file reference
    private String mimeType;  // For files
    private String fileName;  // For files
    private Long fileSize;    // For files
    private String parentContentId;  // To link AI responses to original content
    private String status;  // For determining if done sending to kafka



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserContent userContent = (UserContent) o;
        return Objects.equals(userContentKey, userContent.userContentKey)
                && Objects.equals(contentId, userContent.contentId)
                && Objects.equals(contentType, userContent.contentType)
                && Objects.equals(content, userContent.content)
                && Objects.equals(mimeType, userContent.mimeType)
                && Objects.equals(fileName, userContent.fileName)
                && Objects.equals(fileSize, userContent.fileSize)
                && Objects.equals(parentContentId, userContent.parentContentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userContentKey, contentId, contentType, content, mimeType, fileName, fileSize, parentContentId);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UserContentKey getUserContentKey() {
        return userContentKey;
    }

    public void setUserContentKey(UserContentKey userContentKey) {
        this.userContentKey = userContentKey;
    }

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getParentContentId() {
        return parentContentId;
    }

    public void setParentContentId(String parentContentId) {
        this.parentContentId = parentContentId;
    }

    public static final class UserContentBuilder {
        private UserContentKey userContentKey;
        private String contentId;
        private String contentType;
        private String content;
        private String mimeType;
        private String fileName;
        private Long fileSize;
        private String parentContentId;
        private String status;

        private UserContentBuilder() {
        }

        public static UserContentBuilder anUserContent() {
            return new UserContentBuilder();
        }

        public UserContentBuilder userContentKey(UserContentKey userContentKey) {
            this.userContentKey = userContentKey;
            return this;
        }

        public UserContentBuilder contentId(String contentId) {
            this.contentId = contentId;
            return this;
        }

        public UserContentBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public UserContentBuilder content(String content) {
            this.content = content;
            return this;
        }

        public UserContentBuilder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public UserContentBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public UserContentBuilder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public UserContentBuilder parentContentId(String parentContentId) {
            this.parentContentId = parentContentId;
            return this;
        }

        public UserContentBuilder status(String status) {
            this.status = status;
            return this;
        }

        public UserContent build() {
            UserContent userContent = new UserContent();
            userContent.setUserContentKey(userContentKey);
            userContent.setContentId(contentId);
            userContent.setContentType(contentType);
            userContent.setContent(content);
            userContent.setMimeType(mimeType);
            userContent.setFileName(fileName);
            userContent.setFileSize(fileSize);
            userContent.setParentContentId(parentContentId);
            userContent.setStatus(status);
            return userContent;
        }
    }
}
