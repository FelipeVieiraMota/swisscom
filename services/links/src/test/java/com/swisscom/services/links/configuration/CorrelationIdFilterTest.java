package com.swisscom.services.links.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldPreserveCorrelationIdFromHeader() throws Exception {
        final var request = new MockHttpServletRequest("GET", "/r/code");
        final var response = new MockHttpServletResponse();
        final var correlationIdInsideChain = new AtomicReference<String>();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "request-correlation-id");

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                correlationIdInsideChain.set(MDC.get(CorrelationIdFilter.MDC_KEY))
        );

        assertThat(correlationIdInsideChain).hasValue("request-correlation-id");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo("request-correlation-id");
        assertThat(request.getAttribute(CorrelationIdFilter.MDC_KEY))
                .isEqualTo("request-correlation-id");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void shouldGenerateCorrelationIdWhenItIsMissing() throws Exception {
        final var request = new MockHttpServletRequest("GET", "/api/v1/links");
        final var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> { });

        final String correlationId = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertThat(correlationId).isNotBlank();
        assertThat(UUID.fromString(correlationId)).isNotNull();
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void shouldUseCorrelationIdFromQueryParameterAsFallback() throws Exception {
        final var request = new MockHttpServletRequest("GET", "/r/code");
        final var response = new MockHttpServletResponse();
        request.addParameter(CorrelationIdFilter.MDC_KEY, "query-correlation-id");

        filter.doFilter(request, response, (servletRequest, servletResponse) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo("query-correlation-id");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
