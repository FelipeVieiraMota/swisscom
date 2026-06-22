package com.swisscom.services.links.service;

import com.swisscom.services.links.domain.dto.request.CreateLinkRequest;
import com.swisscom.services.links.domain.dto.response.LinkResponse;
import com.swisscom.services.links.domain.entity.Link;
import com.swisscom.services.links.exception.InvalidLinkUrlException;
import com.swisscom.services.links.exception.LinkNotFoundException;
import com.swisscom.services.links.exception.ShortCodeGenerationException;
import com.swisscom.services.links.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
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
@Slf4j
public class LinkService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private final LinkRepository linkRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final Clock clock;
    private final CacheManager cacheManager;

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
        evictOneLinkCache(link.getShortCode());
    }

    @CacheEvict(value = "popular-links", key = "#shortCode")
    public void evictOneLinkCache(final String shortCode) {
        log.info("Deleting old cache for #shortCode Key : {} ", shortCode);
    }

    @Scheduled(fixedRateString = "${spring.cache.redis.time-to-live}")
    public void clearAllPopularLinksCache() {
        final Cache cache = cacheManager.getCache("popular-links");

        if (cache != null) {
            cache.clear();
            log.info("popular-links cache cleared");
        }
    }

    @Transactional
    public URI resolve(final String shortCode) {
        final String originalUrl = findOriginalUrlByShortCode(shortCode);

        linkRepository.findByShortCodeAndActiveTrue(shortCode)
                .ifPresent(link -> linkRepository.incrementClickCount(link.getId()));

        return URI.create(originalUrl);
    }

    @Cacheable(value = "popular-links", key = "#shortCode")
    public String findOriginalUrlByShortCode(final String shortCode) {
        final Link link = linkRepository.findByShortCodeAndActiveTrue(shortCode)
                .filter(this::isNotExpired)
                .orElseThrow(LinkNotFoundException::new);

        return link.getOriginalUrl();
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
