package com.swisscom.services.auth_service.domain.dto.response;

import com.swisscom.services.auth_service.domain.entity.User;
import com.swisscom.services.auth_service.domain.enums.RoleName;

import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        boolean emailVerified,
        Set<RoleName> roles
) {

    public static UserResponse from(final User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.isEmailVerified(),
                Set.copyOf(user.getRoles())
        );
    }
}
