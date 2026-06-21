package com.swisscom.services.links.domain.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(List<String> publicPaths) {

    public SecurityProperties {
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
    }
}
