package com.procureai.vendor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "vendor_performance_history")
public class VendorPerformanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    private Long purchaseOrderId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal onTimeDeliveryPct;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal qualityScore;

    @Column(nullable = false)
    private Integer disputeCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public BigDecimal getOnTimeDeliveryPct() {
        return onTimeDeliveryPct;
    }

    public void setOnTimeDeliveryPct(BigDecimal onTimeDeliveryPct) {
        this.onTimeDeliveryPct = onTimeDeliveryPct;
    }

    public BigDecimal getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(BigDecimal qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Integer getDisputeCount() {
        return disputeCount;
    }

    public void setDisputeCount(Integer disputeCount) {
        this.disputeCount = disputeCount;
    }
}
