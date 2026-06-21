package com.swisscom.services.links.domain.dto.response;

import com.swisscom.services.links.domain.entity.Link;

import java.time.Instant;
import java.util.UUID;

public record LinkResponse(
        UUID id,
        String shortCode,
        String originalUrl,
        String redirectPath,
        boolean active,
        long clickCount,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static LinkResponse from(final Link link) {
        return new LinkResponse(
                link.getId(),
                link.getShortCode(),
                link.getOriginalUrl(),
                "/r/" + link.getShortCode(),
                link.isActive(),
                link.getClickCount(),
                link.getExpiresAt(),
                link.getCreatedAt(),
                link.getUpdatedAt()
        );
    }
}
