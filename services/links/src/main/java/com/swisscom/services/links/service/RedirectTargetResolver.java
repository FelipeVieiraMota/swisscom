package com.swisscom.services.links.service;

import com.swisscom.services.links.domain.dto.response.RedirectTarget;
import com.swisscom.services.links.domain.entity.Link;
import com.swisscom.services.links.exception.LinkNotFoundException;
import com.swisscom.services.links.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RedirectTargetResolver {

    private final LinkRepository linkRepository;
    private final Clock clock;

    @Cacheable(value = "popular-links", key = "#shortCode")
    public RedirectTarget findByShortCode(final String shortCode) {
        final Link link = linkRepository.findByShortCodeAndActiveTrue(shortCode)
                .filter(this::isNotExpired)
                .orElseThrow(LinkNotFoundException::new);

        return new RedirectTarget(link.getId(), link.getOriginalUrl());
    }

    private boolean isNotExpired(final Link link) {
        return link.getExpiresAt() == null || link.getExpiresAt().isAfter(clock.instant());
    }
}
