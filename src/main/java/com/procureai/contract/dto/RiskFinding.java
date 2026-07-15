package com.procureai.contract.dto;

public record RiskFinding(
        String clauseType,
        String severity,
        String summary,
        Long chunkId,
        String citation
) {
}
