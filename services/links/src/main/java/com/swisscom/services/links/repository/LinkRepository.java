package com.swisscom.services.links.repository;

import com.swisscom.services.links.domain.entity.Link;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkRepository extends JpaRepository<@NonNull Link, @NonNull UUID> {

    boolean existsByShortCode(String shortCode);

    Optional<Link> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Link> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Optional<Link> findByShortCodeAndActiveTrue(String shortCode);
}
