package com.procureai.purchaseorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PurchaseOrderRequest(
        @NotNull Long vendorId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String itemsJson,
        String approverChain
) {
}
