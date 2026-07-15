package com.procureai.purchaseorder.dto;

import com.procureai.purchaseorder.PurchaseOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PurchaseOrderResponse(
        Long id,
        Long vendorId,
        String vendorName,
        PurchaseOrderStatus status,
        String approverChain,
        String createdBy,
        BigDecimal amount,
        String itemsJson,
        Instant createdAt,
        Instant updatedAt,
        List<ApprovalStepResponse> approvalSteps
) {
}
