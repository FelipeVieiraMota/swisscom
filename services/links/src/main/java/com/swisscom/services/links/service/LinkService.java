package com.swisscom.services.links.service;

import com.swisscom.services.links.domain.dto.request.CreateLinkRequest;
import com.swisscom.services.links.domain.dto.response.LinkResponse;
import com.swisscom.services.links.domain.entity.Link;
import com.swisscom.services.links.exception.InvalidLinkUrlException;
import com.swisscom.services.links.exception.LinkNotFoundException;
import com.swisscom.services.links.exception.ShortCodeGenerationException;
import com.swisscom.services.links.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LinkService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final LinkRepository linkRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final Clock clock;

    @Transactional
    public LinkResponse create(final UUID ownerId, final CreateLinkRequest request) {
        final URI originalUri = validateUrl(request.originalUrl());
        validateExpiration(request.expiresAt());

        final Link link = Link.builder()
                .shortCode(nextUniqueShortCode())
                .originalUrl(originalUri.toASCIIString())
                .ownerId(ownerId)
                .expiresAt(request.expiresAt())
                .build();

        return LinkResponse.from(linkRepository.save(link));
    }

    public List<LinkResponse> listByOwner(final UUID ownerId) {
        return linkRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(LinkResponse::from)
                .toList();
    }

    public LinkResponse findByOwner(final UUID ownerId, final UUID linkId) {
        return linkRepository.findByIdAndOwnerId(linkId, ownerId)
                .map(LinkResponse::from)
                .orElseThrow(LinkNotFoundException::new);
    }

    @Transactional
    public void deactivate(final UUID ownerId, final UUID linkId) {
        final Link link = linkRepository.findByIdAndOwnerId(linkId, ownerId)
                .orElseThrow(LinkNotFoundException::new);
        link.setActive(false);
    }

    @Transactional
    public URI resolve(final String shortCode) {
        final Link link = linkRepository.findByShortCodeAndActiveTrue(shortCode)
                .filter(this::isNotExpired)
                .orElseThrow(LinkNotFoundException::new);

        linkRepository.incrementClickCount(link.getId());
        return URI.create(link.getOriginalUrl());
    }

    private String nextUniqueShortCode() {
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            final String candidate = shortCodeGenerator.generate();
            if (!linkRepository.existsByShortCode(candidate)) {
                return candidate;
            }
        }
        throw new ShortCodeGenerationException();
    }

    private URI validateUrl(final String value) {
        try {
            final URI uri = URI.create(value.trim());
            final String scheme = uri.getScheme() == null
                    ? ""
                    : uri.getScheme().toLowerCase(Locale.ROOT);

            if (!ALLOWED_SCHEMES.contains(scheme) || uri.getHost() == null) {
                throw new InvalidLinkUrlException();
            }
            return uri.normalize();
        } catch (IllegalArgumentException exception) {
            throw new InvalidLinkUrlException();
        }
    }

    private void validateExpiration(final Instant expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(clock.instant())) {
            throw new IllegalArgumentException("Expiration must be in the future");
        }
    }

    private boolean isNotExpired(final Link link) {
        return link.getExpiresAt() == null || link.getExpiresAt().isAfter(clock.instant());
    }
}
