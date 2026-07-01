package com.swisscom.services.links.service;

import com.swisscom.services.links.domain.events.ClickCountEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitClickCountEventPublisher implements ClickCountEventPublisher {

    private static final String CLICK_COUNT_EXCHANGE = "links.clicks";
    private static final String CLICK_COUNT_ROUTING_KEY = "links.click-count.increment";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(final UUID linkId) {
        try {
            log.info("Forwarding new click count Id={} for RabbitMQ.", linkId.toString());
            rabbitTemplate.convertAndSend(
                    CLICK_COUNT_EXCHANGE,
                    CLICK_COUNT_ROUTING_KEY,
                    linkId.toString()
            );
        } catch (AmqpException exception) {
            log.warn("Could not enqueue click count increment for link {}", linkId, exception);
        }
    }
}
