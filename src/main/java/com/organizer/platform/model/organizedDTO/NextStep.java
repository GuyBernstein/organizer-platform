package com.organizer.platform.model.organizedDTO;


import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * NextStep represents action items or follow-up tasks associated with a WhatsApp message.
 * Each message can have multiple next steps, creating a task-like structure for message follow-ups.
 * Used in conjunction with WhatsAppMessage to track required actions derived from message content.
 */
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
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private WhatsAppMessage message;

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

    public WhatsAppMessage getMessage() {
        return message;
    }

    public void setMessage(WhatsAppMessage message) {
        this.message = message;
    }

    public static final class NextStepBuilder {
        private Long id;
        private String name;
        private WhatsAppMessage message;

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

        public NextStepBuilder message(WhatsAppMessage messages) {
            this.message = messages;
            return this;
        }

        public NextStep build() {
            NextStep nextStep = new NextStep();
            nextStep.setId(id);
            nextStep.setName(name);
            nextStep.setMessage(message);
            return nextStep;
        }
    }
}

