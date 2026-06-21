package com.swisscom.services.auth_service.service.interfaces;

import com.swisscom.services.auth_service.domain.security.JwtPrincipal;
import org.springframework.security.core.Authentication;

public interface JwtService {

    String generateAccessToken(JwtPrincipal principal);

    Authentication parseAuthentication(String token);
}
