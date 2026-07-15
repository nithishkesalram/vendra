package com.procureai.purchaseorder.dto;

import com.procureai.auth.Role;
import com.procureai.purchaseorder.ApprovalStepStatus;
import java.time.Instant;

public record ApprovalStepResponse(
        Long id,
        Role approverRole,
        ApprovalStepStatus status,
        Instant actedAt,
        String comments
) {
}
