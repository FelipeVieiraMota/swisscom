package com.swisscom.services.links.domain.dto.response;

import java.io.Serializable;
import java.util.UUID;

public record RedirectTarget(
        UUID linkId,
        String originalUrl
) implements Serializable {
}
