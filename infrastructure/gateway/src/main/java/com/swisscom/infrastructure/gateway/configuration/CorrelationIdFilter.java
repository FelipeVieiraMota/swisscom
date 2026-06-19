package com.swisscom.infrastructure.gateway.configuration;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Consumer;

@Component
@Slf4j
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    @NonNull
    public Mono<@NonNull Void> filter(
            @NonNull final ServerWebExchange exchange,
            @NonNull final GatewayFilterChain chain
    ){

        String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = exchange.getRequest().getQueryParams().getFirst("correlationId");
        }

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        String finalCorrelationId = correlationId;

        var request = exchange.getRequest()
                .mutate()
                .headers(httpHeadersConsumer(finalCorrelationId))
                .build();

        var mutatedExchange = exchange.mutate().request(request).build();
        mutatedExchange.getAttributes().put(MDC_KEY, finalCorrelationId);
        mutatedExchange.getResponse().getHeaders().set(HEADER_NAME, finalCorrelationId);

        MDC.put(MDC_KEY, finalCorrelationId);
        log.info("Incoming request {} {}", request.getMethod(), request.getURI());

        return chain
                .filter(mutatedExchange)
                .doFinally(signalType -> MDC.remove(MDC_KEY));
    }

    private Consumer<HttpHeaders> httpHeadersConsumer(String finalCorrelationId) {
        return headers -> headers.set(HEADER_NAME, finalCorrelationId);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
