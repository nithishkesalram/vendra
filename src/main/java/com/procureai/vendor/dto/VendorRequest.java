package com.procureai.vendor.dto;

import com.procureai.vendor.ComplianceStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record VendorRequest(
        @NotBlank String name,
        @NotBlank String category,
        @NotNull @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal rating,
        @NotNull ComplianceStatus complianceStatus,
        @Min(0) @Max(100) Integer riskScore
) {
}
