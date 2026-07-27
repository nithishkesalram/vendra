package com.procureai.purchaseorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record PurchaseOrderRequest(
        @NotNull Long vendorId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @Valid List<PurchaseOrderLineItemRequest> lineItems,
        String itemsJson,
        String approverChain
) {
}
