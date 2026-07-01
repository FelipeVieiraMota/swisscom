package com.swisscom.services.link_consumer.service;

import com.swisscom.services.link_consumer.configuration.ClickCountMessagingConfiguration;
import com.swisscom.services.link_consumer.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClickCountEventConsumer {

    private final LinkRepository linkRepository;

    @RabbitListener(queues = ClickCountMessagingConfiguration.CLICK_COUNT_QUEUE)
    @Transactional
    public void incrementClickCount(final String linkId) {
        final UUID id = UUID.fromString(linkId);
        final int updatedRows = linkRepository.incrementClickCount(id);

        if (updatedRows == 0) {
            log.warn("Could not increment click count because link {} was not found", id);
        }
    }
}
