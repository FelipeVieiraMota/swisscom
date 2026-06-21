package com.swisscom.services.auth_service.domain.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}
