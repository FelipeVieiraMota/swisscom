package com.swisscom.services.links.domain.events;

import java.util.UUID;

public interface ClickCountEventPublisher {

    void publish(UUID linkId);
}
