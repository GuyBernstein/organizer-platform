package com.organizer.platform.model.organizedDTO;


import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "next_steps")
public class NextStep implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @JsonIgnore
    @ManyToMany(mappedBy = "nextSteps")
    private Set<WhatsAppMessage> messages = new HashSet<>();

    // Add proper getters and setters
    public Set<WhatsAppMessage> getMessages() {
        if (messages == null) {
            messages = new HashSet<>();
        }
        return messages;
    }

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

    public void setMessages(Set<WhatsAppMessage> messages) {
        this.messages = messages;
    }

    public static final class NextStepBuilder {
        private Long id;
        private String name;
        private Set<WhatsAppMessage> messages;

        private NextStepBuilder() {
        }

        public static NextStepBuilder aNextStep() {
            return new NextStepBuilder();
        }

        public NextStepBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public NextStepBuilder name(String name) {
            this.name = name;
            return this;
        }

        public NextStepBuilder messages(Set<WhatsAppMessage> messages) {
            this.messages = messages;
            return this;
        }

        public NextStep build() {
            NextStep nextStep = new NextStep();
            nextStep.setId(id);
            nextStep.setName(name);
            nextStep.setMessages(messages);
            return nextStep;
        }
    }
}

