package com.procureai.infra.kafka;

import java.time.Instant;
import java.util.UUID;

public record DocumentUploadedEvent(
        UUID eventId,
        Long contractId,
        Long vendorId,
        int chunksCreated,
        String actor,
        Instant occurredAt
) {
}
