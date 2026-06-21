package com.swisscom.services.links;

import com.swisscom.services.links.domain.entity.Link;
import com.swisscom.services.links.repository.LinkRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LinksApplicationTests {

    private static final String TEST_SECRET = "YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LinkRepository linkRepository;

    @BeforeEach
    void cleanDatabase() {
        linkRepository.deleteAll();
    }

    @Test
    void shouldRejectLinkCreationWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl":"https://example.com/article"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateAndListOwnedLink() throws Exception {
        final UUID ownerId = UUID.randomUUID();
        final String token = tokenFor(ownerId);

        mockMvc.perform(post("/api/v1/links")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl":"https://example.com/articles/one"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").isNotEmpty())
                .andExpect(jsonPath("$.redirectPath").value(org.hamcrest.Matchers.startsWith("/r/")))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.clickCount").value(0));

        mockMvc.perform(get("/api/v1/links")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalUrl").value("https://example.com/articles/one"));
    }

    @Test
    void shouldNotExposeAnotherUsersLink() throws Exception {
        final Link link = linkRepository.save(Link.builder()
                .shortCode("Owner123")
                .originalUrl("https://example.com/private")
                .ownerId(UUID.randomUUID())
                .build());

        mockMvc.perform(get("/api/v1/links/{id}", link.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRedirectPublicLinkAndIncrementClickCount() throws Exception {
        final Link link = linkRepository.save(Link.builder()
                .shortCode("GoThere1")
                .originalUrl("https://example.com/destination")
                .ownerId(UUID.randomUUID())
                .build());

        mockMvc.perform(get("/r/{shortCode}", link.getShortCode()))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/destination"));

        assertThat(linkRepository.findById(link.getId()).orElseThrow().getClickCount()).isEqualTo(1);
    }

    @Test
    void shouldRejectInvalidTargetUrl() throws Exception {
        mockMvc.perform(post("/api/v1/links")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl":"javascript:alert(1)"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only absolute HTTP or HTTPS URLs are supported"));
    }

    @Test
    void shouldDeactivateLink() throws Exception {
        final UUID ownerId = UUID.randomUUID();
        final Link link = linkRepository.save(Link.builder()
                .shortCode("Disable1")
                .originalUrl("https://example.com/disabled")
                .ownerId(ownerId)
                .build());

        mockMvc.perform(delete("/api/v1/links/{id}", link.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(ownerId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/r/{shortCode}", link.getShortCode()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotRedirectExpiredLink() throws Exception {
        final Link link = linkRepository.save(Link.builder()
                .shortCode("Expired1")
                .originalUrl("https://example.com/expired")
                .ownerId(UUID.randomUUID())
                .expiresAt(Instant.now().minusSeconds(60))
                .build());

        mockMvc.perform(get("/r/{shortCode}", link.getShortCode()))
                .andExpect(status().isNotFound());
    }

    private String tokenFor(final UUID ownerId) {
        final Instant now = Instant.now();
        return Jwts.builder()
                .issuer("swisscom-authenticator")
                .audience().add("swisscom-api").and()
                .subject(ownerId.toString())
                .claim("email", "owner@example.com")
                .claim("roles", List.of("USER"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)), Jwts.SIG.HS256)
                .compact();
    }
}
