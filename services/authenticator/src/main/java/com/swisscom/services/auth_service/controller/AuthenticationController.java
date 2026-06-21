package com.swisscom.services.auth_service.controller;

import com.swisscom.services.auth_service.configuration.OpenApiConfiguration;
import com.swisscom.services.auth_service.domain.dto.request.LoginRequest;
import com.swisscom.services.auth_service.domain.dto.request.RegisterRequest;
import com.swisscom.services.auth_service.domain.dto.response.AuthResponse;
import com.swisscom.services.auth_service.domain.dto.response.UserResponse;
import com.swisscom.services.auth_service.domain.security.JwtPrincipal;
import com.swisscom.services.auth_service.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and JWT authentication")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a user", description = "Requires the ADMIN role")
    @SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
    public UserResponse register(@Valid @RequestBody final RegisterRequest request) {
        return authenticationService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and issue an access token")
    public AuthResponse login(@Valid @RequestBody final LoginRequest request) {
        return authenticationService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Return the authenticated user")
    @SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
    public JwtPrincipal me(@AuthenticationPrincipal final JwtPrincipal principal) {
        return principal;
    }
}
