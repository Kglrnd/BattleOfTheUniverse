package de.kugi.dev.battleoftheuniverse.user.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateLanguageRequest(
        @Pattern(regexp = "en|de") String language
) {
}
