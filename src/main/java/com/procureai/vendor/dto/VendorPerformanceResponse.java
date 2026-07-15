package com.procureai.vendor.dto;

import java.math.BigDecimal;

public record VendorPerformanceResponse(
        Long vendorId,
        String vendorName,
        BigDecimal averageOnTimeDeliveryPct,
        BigDecimal averageQualityScore,
        int disputeCount,
        int sampleSize
) {
}
