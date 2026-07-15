package com.procureai.contract.dto;

import com.procureai.contract.RiskLevel;
import java.util.List;

public record ContractRiskResponse(
        Long contractId,
        Long vendorId,
        RiskLevel riskLevel,
        int riskScore,
        List<RiskFinding> findings
) {
}
