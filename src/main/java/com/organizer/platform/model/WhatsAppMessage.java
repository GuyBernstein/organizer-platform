package com.organizer.platform.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.organizer.platform.util.Dates;
import org.joda.time.LocalDateTime;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name="whatsapp_message")
public class WhatsAppMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Date createdAt = Dates.nowUTC();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("createdAt")
    public LocalDateTime calcCreatedAt() {
        return Dates.atLocalTime(createdAt);
    }


    @NotEmpty
    @Column(nullable = false)
    private String fromNumber;

    @NotEmpty
    @Column(nullable = false)
    private String messageType;  // text, image, document, etc.

    @Column(columnDefinition = "TEXT")
    private String messageContent;

    @Column(length = 500)
    private String mediaUrl;  // URL for stored media files

    @Column(nullable = false)
    private boolean processed = false;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getFromNumber() {
        return fromNumber;
    }

    public void setFromNumber(String fromNumber) {
        this.fromNumber = fromNumber;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public static final class WhatsAppMessageBuilder {
        private Long id;
        private Date createdAt = Dates.nowUTC();
        private @NotEmpty String fromNumber;
        private @NotEmpty String messageType;
        private String messageContent;
        private String mediaUrl;
        private boolean processed;

        private WhatsAppMessageBuilder() {
        }

        public static WhatsAppMessageBuilder aWhatsAppMessage() {
            return new WhatsAppMessageBuilder();
        }

        public WhatsAppMessageBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public WhatsAppMessageBuilder createdAt(Date createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public WhatsAppMessageBuilder fromNumber(String fromNumber) {
            this.fromNumber = fromNumber;
            return this;
        }

        public WhatsAppMessageBuilder messageType(String messageType) {
            this.messageType = messageType;
            return this;
        }

        public WhatsAppMessageBuilder messageContent(String messageContent) {
            this.messageContent = messageContent;
            return this;
        }

        public WhatsAppMessageBuilder mediaUrl(String mediaUrl) {
            this.mediaUrl = mediaUrl;
            return this;
        }

        public WhatsAppMessageBuilder processed(boolean processed) {
            this.processed = processed;
            return this;
        }

        public WhatsAppMessage build() {
            WhatsAppMessage whatsAppMessage = new WhatsAppMessage();
            whatsAppMessage.setId(id);
            whatsAppMessage.setCreatedAt(createdAt);
            whatsAppMessage.setFromNumber(fromNumber);
            whatsAppMessage.setMessageType(messageType);
            whatsAppMessage.setMessageContent(messageContent);
            whatsAppMessage.setMediaUrl(mediaUrl);
            whatsAppMessage.setProcessed(processed);
            return whatsAppMessage;
        }
    }
}
