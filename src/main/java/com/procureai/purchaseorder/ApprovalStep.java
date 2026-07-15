package com.procureai.purchaseorder;

import com.procureai.auth.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "approval_steps")
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id")
    private PurchaseOrder purchaseOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role approverRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStepStatus status = ApprovalStepStatus.PENDING;

    private Instant actedAt;

    private String comments;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public Role getApproverRole() {
        return approverRole;
    }

    public void setApproverRole(Role approverRole) {
        this.approverRole = approverRole;
    }

    public ApprovalStepStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStepStatus status) {
        this.status = status;
    }

    public Instant getActedAt() {
        return actedAt;
    }

    public void setActedAt(Instant actedAt) {
        this.actedAt = actedAt;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
