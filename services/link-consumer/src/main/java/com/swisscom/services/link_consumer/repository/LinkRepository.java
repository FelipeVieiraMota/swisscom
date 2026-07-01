package com.swisscom.services.link_consumer.repository;

import com.swisscom.services.link_consumer.domain.entity.Link;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface LinkRepository extends JpaRepository<@NonNull Link, @NonNull UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Link link set link.clickCount = link.clickCount + 1 where link.id = :id")
    int incrementClickCount(@Param("id") UUID id);
}
