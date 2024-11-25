package com.organizer.platform.model.organizedDTO;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
public class Tag implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @JsonIgnore
    @ManyToMany(mappedBy = "tags")
    private Set<WhatsAppMessage> messages = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<WhatsAppMessage> getMessages() {
        return messages;
    }

    public void setMessages(Set<WhatsAppMessage> messages) {
        this.messages = messages;
    }

    public static final class TagBuilder {
        private Long id;
        private String name;
        private Set<WhatsAppMessage> messages = new HashSet<>();

        private TagBuilder() {
        }

        public static TagBuilder aTag() {
            return new TagBuilder();
        }

        public TagBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public TagBuilder name(String name) {
            this.name = name;
            return this;
        }

        public TagBuilder messages(Set<WhatsAppMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Tag build() {
            Tag tag = new Tag();
            tag.setId(id);
            tag.setName(name);
            tag.setMessages(messages);
            return tag;
        }
    }
}
