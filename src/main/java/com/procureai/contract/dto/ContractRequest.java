package com.procureai.contract.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ContractRequest(
        @NotNull Long vendorId,
        String documentUrl,
        LocalDate expiryDate
) {
}
