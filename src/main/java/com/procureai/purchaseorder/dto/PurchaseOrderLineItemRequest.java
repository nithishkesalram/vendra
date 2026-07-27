package com.procureai.purchaseorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseOrderLineItemRequest(
        @NotBlank String productName,
        @NotNull @Positive Integer quantity
) {
}
