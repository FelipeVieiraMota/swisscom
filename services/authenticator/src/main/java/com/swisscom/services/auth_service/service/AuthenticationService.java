package com.swisscom.services.auth_service.service;

import com.swisscom.services.auth_service.domain.dto.request.LoginRequest;
import com.swisscom.services.auth_service.domain.dto.request.RegisterRequest;
import com.swisscom.services.auth_service.domain.dto.response.AuthResponse;
import com.swisscom.services.auth_service.domain.dto.response.UserResponse;
import com.swisscom.services.auth_service.domain.entity.User;
import com.swisscom.services.auth_service.domain.properties.JwtProperties;
import com.swisscom.services.auth_service.domain.security.JwtPrincipal;
import com.swisscom.services.auth_service.exception.EmailAlreadyRegisteredException;
import com.swisscom.services.auth_service.repository.UserRepository;
import com.swisscom.services.auth_service.service.interfaces.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public UserResponse register(final RegisterRequest request) {
        final String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        final User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .build());

        return UserResponse.from(user);
    }

    @Transactional
    public AuthResponse login(final LoginRequest request) {
        final String email = normalizeEmail(request.email());
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.password())
        );

        final User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        user.setLastLoginAt(clock.instant());

        final JwtPrincipal principal = new JwtPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet())
        );
        final String accessToken = jwtService.generateAccessToken(principal);

        return new AuthResponse(
                accessToken,
                TOKEN_TYPE,
                jwtProperties.accessTokenTtl().toSeconds()
        );
    }

    private static String normalizeEmail(final String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
