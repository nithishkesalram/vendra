package com.procureai.contract.dto;

import com.procureai.contract.RiskLevel;
import java.time.LocalDate;

public record ContractResponse(
        Long id,
        Long vendorId,
        String vendorName,
        String documentUrl,
        String extractedClausesJson,
        RiskLevel riskLevel,
        LocalDate expiryDate
) {
}
