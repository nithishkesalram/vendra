package com.procureai.contract.dto;

public record DocumentUploadResponse(
        Long contractId,
        Long vendorId,
        int chunksCreated,
        String documentUrl
) {
}
