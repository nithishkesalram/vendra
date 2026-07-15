package com.procureai.infra.kafka;

import java.time.Instant;
import java.util.UUID;

public record PoStatusChangedEvent(
        UUID eventId,
        Long purchaseOrderId,
        String previousStatus,
        String newStatus,
        String actor,
        Instant occurredAt
) {
}
