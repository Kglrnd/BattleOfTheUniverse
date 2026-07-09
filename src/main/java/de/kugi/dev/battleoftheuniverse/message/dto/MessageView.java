package de.kugi.dev.battleoftheuniverse.message.dto;

import de.kugi.dev.battleoftheuniverse.message.MessageType;

import java.time.Instant;

public record MessageView(
        Long id,
        String senderUsername,
        String recipientUsername,
        String subject,
        String body,
        MessageType type,
        Instant sentAt,
        Instant readAt
) {
}
