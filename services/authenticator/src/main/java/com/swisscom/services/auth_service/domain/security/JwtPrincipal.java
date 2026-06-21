package com.swisscom.services.auth_service.domain.security;

import java.util.Set;
import java.util.UUID;

public record JwtPrincipal(UUID userId, String email, Set<String> roles) {

    public JwtPrincipal {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
