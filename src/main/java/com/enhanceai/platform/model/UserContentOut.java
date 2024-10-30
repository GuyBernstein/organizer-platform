package com.enhanceai.platform.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.enhanceai.platform.util.Dates;
import org.joda.time.LocalDateTime;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserContentOut {

    private String userName;
    private Date creationTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("creationTime")
    public LocalDateTime calcClickTime() {
        return Dates.atLocalTime(creationTime);
    }
    private String contentId;
    private String contentType;
    private String content;
    private String mimeType;
    private String fileName;
    private Long fileSize;
    private String parentContentId;
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
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

    public static UserContentOut of(UserContent userContent) {
        UserContentOut res = new UserContentOut();
        res.userName = userContent.getUserContentKey().getUserName();
        res.creationTime = userContent.getUserContentKey().getCreationTime();
        res.contentId = userContent.getContentId();
        res.contentType = userContent.getContentType();
        res.content = userContent.getContent();
        if(userContent.getContentType().equals("FILE")) {
            res.mimeType = userContent.getMimeType();
            res.fileName = userContent.getFileName();
            res.fileSize = userContent.getFileSize();
            res.status = userContent.getStatus();
        }
        if(userContent.getContentType().equals("AI"))
            res.parentContentId = userContent.getParentContentId();
        return res;
    }
}
