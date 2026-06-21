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
                        .uri("lb://discovery"))
                .route("dev-spring-boot-admin", route -> route
                        .header("Host", "admin\\.localhost(?::\\d+)?")
                        .uri("lb://observability"))
                .build();
    }
}
