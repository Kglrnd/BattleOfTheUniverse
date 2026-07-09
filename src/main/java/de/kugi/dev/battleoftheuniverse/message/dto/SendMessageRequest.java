package de.kugi.dev.battleoftheuniverse.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank String recipientUsername,
        @NotBlank @Size(max = 120) String subject,
        @NotBlank @Size(max = 4000) String body
) {
}
