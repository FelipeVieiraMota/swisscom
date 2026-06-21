package com.swisscom.services.links.configuration;

import com.swisscom.services.links.domain.properties.JwtProperties;
import com.swisscom.services.links.domain.security.JwtPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtTokenParser {

    private static final String EMAIL_CLAIM = "email";
    private static final String ROLES_CLAIM = "roles";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey key;

    public JwtTokenParser(final JwtProperties properties, final Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    }

    public Authentication parse(final String token) {
        final Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .requireAudience(properties.audience())
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        final Set<String> roles = readRoles(claims);
        final JwtPrincipal principal = new JwtPrincipal(
                UUID.fromString(claims.getSubject()),
                claims.get(EMAIL_CLAIM, String.class),
                roles
        );
        final List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(JwtTokenParser::asAuthority)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
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
