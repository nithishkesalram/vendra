package com.procureai.quotation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record QuotationRequest(
        @NotNull Long vendorId,
        @NotBlank String rfqId,
        @NotBlank String itemsJson,
        @NotNull @DecimalMin("0.01") BigDecimal totalCost,
        @NotNull @Min(1) Integer deliveryDays
) {
}
