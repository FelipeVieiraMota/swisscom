package com.swisscom.services.links.domain.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateLinkRequest(
        @NotBlank
        @Size(max = 2048)
        String originalUrl,

        @Future
        Instant expiresAt
) {
}
