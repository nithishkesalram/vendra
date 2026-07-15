package com.procureai.infra.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PoCreatedEvent(
        UUID eventId,
        Long purchaseOrderId,
        Long vendorId,
        BigDecimal amount,
        String actor,
        Instant occurredAt
) {
}
