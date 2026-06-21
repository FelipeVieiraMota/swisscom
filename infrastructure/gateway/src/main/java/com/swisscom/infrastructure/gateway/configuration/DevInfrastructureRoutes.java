package com.swisscom.infrastructure.gateway.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

@Configuration
@Profile("dev")
public class DevInfrastructureRoutes {

    @Bean
    RouteLocator devInfrastructureRouteLocator(final RouteLocatorBuilder builder) {
        return builder.routes()
                .route("dev-eureka", route -> route
                        .header("Host", "eureka\\.localhost(?::\\d+)?")
                        .uri("http://discovery:8761"))
                .route("dev-spring-boot-admin", route -> route
                        .header("Host", "admin\\.localhost(?::\\d+)?")
                        .uri("http://observability:10000"))
                .route("dev-authenticator-management", route -> route
                        .header("Host", "auth\\.localhost(?::\\d+)?")
                        .uri("http://authenticator:4000"))
                .route("dev-links-management", route -> route
                        .header("Host", "links\\.localhost(?::\\d+)?")
                        .uri("http://links:5000"))
                .build();
    }
}
