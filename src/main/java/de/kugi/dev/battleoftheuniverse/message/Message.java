package de.kugi.dev.battleoftheuniverse.message;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null means this is a {@link MessageType#SYSTEM} message with no player sender. */
    private Long senderUserId;

    private Long recipientUserId;

    @Column(length = 120)
    private String subject;

    @Column(length = 4000)
    private String body;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    private Instant sentAt;

    private Instant readAt;

    public Message(Long senderUserId, Long recipientUserId, String subject, String body, MessageType type, Instant sentAt) {
        this.senderUserId = senderUserId;
        this.recipientUserId = recipientUserId;
        this.subject = subject;
        this.body = body;
        this.type = type;
        this.sentAt = sentAt;
    }
}
