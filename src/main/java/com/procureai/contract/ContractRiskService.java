package com.procureai.contract;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureai.ai.rag.DocumentChunk;
import com.procureai.ai.rag.DocumentChunkRepository;
import com.procureai.audit.AuditLogged;
import com.procureai.contract.dto.ContractRiskResponse;
import com.procureai.contract.dto.RiskFinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractRiskService {

    private final ContractService contractService;
    private final DocumentChunkRepository documentChunkRepository;
    private final ObjectMapper objectMapper;

    public ContractRiskService(
            ContractService contractService,
            DocumentChunkRepository documentChunkRepository,
            ObjectMapper objectMapper
    ) {
        this.contractService = contractService;
        this.documentChunkRepository = documentChunkRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR_MANAGER','PROCUREMENT_OFFICER','APPROVER_L1','APPROVER_L2')")
    @AuditLogged(entityType = "CONTRACT", action = "RISK_ANALYSIS")
    public ContractRiskResponse analyze(Long contractId) {
        Contract contract = contractService.findEntity(contractId);
        List<DocumentChunk> chunks = documentChunkRepository.findBySourceDocId(contractId);
        List<RiskFinding> findings = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            findings.addAll(findRisks(chunk));
        }
        int score = Math.min(100, findings.stream().mapToInt(this::scoreFor).sum());
        RiskLevel riskLevel = toRiskLevel(score);
        contract.setRiskLevel(riskLevel);
        contract.setExtractedClausesJson(toJson(findings));
        return new ContractRiskResponse(contract.getId(), contract.getVendor().getId(), riskLevel, score, findings);
    }

    private List<RiskFinding> findRisks(DocumentChunk chunk) {
        String content = chunk.getContent().toLowerCase(Locale.ROOT);
        List<RiskFinding> findings = new ArrayList<>();
        if (content.contains("unlimited liability") || content.contains("indemnify")) {
            findings.add(finding("liability", "HIGH", "Potentially broad liability or indemnity language.", chunk));
        }
        if (content.contains("terminate for convenience") || content.contains("termination without cause")) {
            findings.add(finding("termination", "MEDIUM", "Termination rights may allow abrupt supplier exit or buyer exposure.", chunk));
        }
        if (content.contains("automatic renewal") || content.contains("auto-renew")) {
            findings.add(finding("renewal", "MEDIUM", "Automatic renewal language needs explicit calendar controls.", chunk));
        }
        if (content.contains("payment due within 7") || content.contains("late payment penalty")) {
            findings.add(finding("payment_terms", "LOW", "Payment timing or penalty language may affect working capital.", chunk));
        }
        return findings;
    }

    private RiskFinding finding(String clauseType, String severity, String summary, DocumentChunk chunk) {
        return new RiskFinding(
                clauseType,
                severity,
                summary,
                chunk.getId(),
                "%s:%d".formatted(chunk.getSourceType().name().toLowerCase(Locale.ROOT), chunk.getPageNumber())
        );
    }

    private int scoreFor(RiskFinding finding) {
        return switch (finding.severity()) {
            case "HIGH" -> 35;
            case "MEDIUM" -> 20;
            default -> 10;
        };
    }

    private RiskLevel toRiskLevel(int score) {
        if (score >= 80) {
            return RiskLevel.CRITICAL;
        }
        if (score >= 50) {
            return RiskLevel.HIGH;
        }
        if (score >= 20) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private String toJson(List<RiskFinding> findings) {
        try {
            return objectMapper.writeValueAsString(findings);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
