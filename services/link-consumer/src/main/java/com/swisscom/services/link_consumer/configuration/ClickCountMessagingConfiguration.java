package com.swisscom.services.link_consumer.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClickCountMessagingConfiguration {

    public static final String CLICK_COUNT_EXCHANGE = "links.clicks";
    public static final String CLICK_COUNT_QUEUE = "links.click-count";
    public static final String CLICK_COUNT_ROUTING_KEY = "links.click-count.increment";

    @Bean
    DirectExchange clickCountExchange() {
        return new DirectExchange(CLICK_COUNT_EXCHANGE, true, false);
    }

    @Bean
    Queue clickCountQueue() {
        return new Queue(CLICK_COUNT_QUEUE, true);
    }

    @Bean
    Binding clickCountBinding(final Queue clickCountQueue, final DirectExchange clickCountExchange) {
        return BindingBuilder
                .bind(clickCountQueue)
                .to(clickCountExchange)
                .with(CLICK_COUNT_ROUTING_KEY);
    }
}
