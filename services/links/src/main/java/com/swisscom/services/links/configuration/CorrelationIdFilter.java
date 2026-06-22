package com.swisscom.services.links.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain filterChain
    ) throws ServletException, IOException {
        final String correlationId = resolveCorrelationId(request);

        request.setAttribute(MDC_KEY, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        MDC.put(MDC_KEY, correlationId);

        try {
            log.info("Incoming request {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveCorrelationId(final HttpServletRequest request) {
        final String headerValue = request.getHeader(HEADER_NAME);
        if (headerValue != null && !headerValue.isBlank()) {
            return headerValue.trim();
        }

        final String queryValue = request.getParameter(MDC_KEY);
        if (queryValue != null && !queryValue.isBlank()) {
            return queryValue.trim();
        }

        return UUID.randomUUID().toString();
    }
}
