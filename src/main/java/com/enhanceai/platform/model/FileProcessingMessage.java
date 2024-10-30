package com.enhanceai.platform.model;

import java.util.Date;

public class FileProcessingMessage {
    private String contentId;
    private String userName;
    private String category;
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private byte[] fileContent;
    private Date creationTime;

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public static final class FileProcessingMessageBuilder {
        private String contentId;
        private String userName;
        private String category;
        private String fileName;
        private String mimeType;
        private Long fileSize;
        private byte[] fileContent;
        private Date creationTime;

        private FileProcessingMessageBuilder() {
        }

        public static FileProcessingMessageBuilder aFileProcessingMessage() {
            return new FileProcessingMessageBuilder();
        }

        public FileProcessingMessageBuilder contentId(String contentId) {
            this.contentId = contentId;
            return this;
        }

        public FileProcessingMessageBuilder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public FileProcessingMessageBuilder category(String category) {
            this.category = category;
            return this;
        }

        public FileProcessingMessageBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public FileProcessingMessageBuilder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public FileProcessingMessageBuilder fileSize(Long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public FileProcessingMessageBuilder fileContent(byte[] fileContent) {
            this.fileContent = fileContent;
            return this;
        }

        public FileProcessingMessageBuilder creationTime(Date creationTime) {
            this.creationTime = creationTime;
            return this;
        }

        public FileProcessingMessage build() {
            FileProcessingMessage fileProcessingMessage = new FileProcessingMessage();
            fileProcessingMessage.setContentId(contentId);
            fileProcessingMessage.setUserName(userName);
            fileProcessingMessage.setCategory(category);
            fileProcessingMessage.setFileName(fileName);
            fileProcessingMessage.setMimeType(mimeType);
            fileProcessingMessage.setFileSize(fileSize);
            fileProcessingMessage.setFileContent(fileContent);
            fileProcessingMessage.setCreationTime(creationTime);
            return fileProcessingMessage;
        }
    }
}
