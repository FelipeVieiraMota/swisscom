package com.swisscom.services.links.configuration;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenParser jwtTokenParser;

    @Override
    protected void doFilterInternal(
                 final HttpServletRequest request,
        @NonNull final HttpServletResponse response,
        @NonNull final FilterChain filterChain
    ) throws ServletException, IOException {
        final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String token = authorization.substring(BEARER_PREFIX.length()).trim();
            final var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(jwtTokenParser.parse(token));
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid bearer token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
