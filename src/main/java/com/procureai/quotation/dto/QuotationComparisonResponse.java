package com.procureai.quotation.dto;

import java.math.BigDecimal;

public record QuotationComparisonResponse(
        Long quotationId,
        Long vendorId,
        String vendorName,
        String rfqId,
        BigDecimal totalCost,
        Integer deliveryDays,
        BigDecimal aiScore,
        String rationale
) {
}
