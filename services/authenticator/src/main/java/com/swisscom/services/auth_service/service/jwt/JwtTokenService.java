package com.swisscom.services.auth_service.service.jwt;

import com.swisscom.services.auth_service.domain.properties.JwtProperties;
import com.swisscom.services.auth_service.domain.security.JwtPrincipal;
import com.swisscom.services.auth_service.service.interfaces.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtTokenService implements JwtService {

    private static final String EMAIL_CLAIM = "email";
    private static final String ROLES_CLAIM = "roles";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey key;

    public JwtTokenService(final JwtProperties properties, final Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    }

    @Override
    public String generateAccessToken(final JwtPrincipal principal) {
        final Instant issuedAt = clock.instant();
        final Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.issuer())
                .audience().add(properties.audience()).and()
                .subject(principal.userId().toString())
                .claim(EMAIL_CLAIM, principal.email())
                .claim(ROLES_CLAIM, principal.roles())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public Authentication parseAuthentication(final String token) {
        final Claims claims = parseClaims(token);
        final Set<String> roles = readRoles(claims);
        final JwtPrincipal principal = new JwtPrincipal(
                UUID.fromString(claims.getSubject()),
                claims.get(EMAIL_CLAIM, String.class),
                roles
        );

        final List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(JwtTokenService::asAuthority)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private Claims parseClaims(final String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .requireAudience(properties.audience())
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static Set<String> readRoles(final Claims claims) {
        final Collection<?> roles = claims.get(ROLES_CLAIM, Collection.class);
        if (roles == null) {
            return Set.of();
        }

        return roles.stream()
                .map(String::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String asAuthority(final String role) {
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}
