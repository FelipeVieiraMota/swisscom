package com.swisscom.services.links.controller;

import com.swisscom.services.links.service.LinkService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/r")
@RequiredArgsConstructor
public class RedirectController {

    private final LinkService linkService;

    @GetMapping("/{shortCode}")
    @Operation(summary = "Redirect a public short link")
    public ResponseEntity<@NonNull Void> redirect(@PathVariable final String shortCode) {
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(linkService.resolve(shortCode))
                .build();
    }
}
