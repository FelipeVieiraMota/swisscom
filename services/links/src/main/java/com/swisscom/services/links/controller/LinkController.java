package com.swisscom.services.links.controller;

import com.swisscom.services.links.domain.dto.request.CreateLinkRequest;
import com.swisscom.services.links.domain.dto.response.LinkResponse;
import com.swisscom.services.links.domain.security.JwtPrincipal;
import com.swisscom.services.links.service.LinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class LinkController {

    private final LinkService linkService;

    @PostMapping
    @Operation(summary = "Create a short link")
    public ResponseEntity<@NonNull LinkResponse> create(
            @AuthenticationPrincipal final JwtPrincipal principal,
            @Valid @RequestBody final CreateLinkRequest request
    ) {
        final LinkResponse response = linkService.create(principal.userId(), request);
        return ResponseEntity
                .created(URI.create("/api/v1/links/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "List the current user's links")
    public List<LinkResponse> list(@AuthenticationPrincipal final JwtPrincipal principal) {
        return linkService.listByOwner(principal.userId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one link owned by the current user")
    public LinkResponse find(
            @AuthenticationPrincipal final JwtPrincipal principal,
            @PathVariable final UUID id
    ) {
        return linkService.findByOwner(principal.userId(), id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate one link owned by the current user")
    public ResponseEntity<@NonNull Void> deactivate(
            @AuthenticationPrincipal final JwtPrincipal principal,
            @PathVariable final UUID id
    ) {
        linkService.deactivate(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
