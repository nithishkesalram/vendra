package com.procureai.quotation.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record QuotationResponse(
        Long id,
        Long vendorId,
        String vendorName,
        String rfqId,
        String itemsJson,
        BigDecimal totalCost,
        Integer deliveryDays,
        Instant submittedAt,
        BigDecimal aiScore
) {
}
