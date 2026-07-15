package com.procureai.vendor.dto;

import com.procureai.vendor.ComplianceStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record VendorResponse(
        Long id,
        String name,
        String category,
        BigDecimal rating,
        ComplianceStatus complianceStatus,
        Instant onboardedAt,
        Integer riskScore
) {
}
