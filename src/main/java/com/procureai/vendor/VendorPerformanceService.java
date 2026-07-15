package com.procureai.vendor;

import com.procureai.vendor.dto.VendorPerformanceResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorPerformanceService {

    private final VendorService vendorService;
    private final VendorPerformanceRepository performanceRepository;

    public VendorPerformanceService(VendorService vendorService, VendorPerformanceRepository performanceRepository) {
        this.vendorService = vendorService;
        this.performanceRepository = performanceRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR_MANAGER','PROCUREMENT_OFFICER','APPROVER_L1','APPROVER_L2')")
    public VendorPerformanceResponse getPerformance(Long vendorId) {
        Vendor vendor = vendorService.findEntity(vendorId);
        List<VendorPerformanceHistory> rows = performanceRepository.findByVendorId(vendorId);
        if (rows.isEmpty()) {
            return new VendorPerformanceResponse(
                    vendor.getId(),
                    vendor.getName(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0,
                    0
            );
        }
        BigDecimal onTime = rows.stream()
                .map(VendorPerformanceHistory::getOnTimeDeliveryPct)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP);
        BigDecimal quality = rows.stream()
                .map(VendorPerformanceHistory::getQualityScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP);
        int disputes = rows.stream().mapToInt(VendorPerformanceHistory::getDisputeCount).sum();
        return new VendorPerformanceResponse(vendor.getId(), vendor.getName(), onTime, quality, disputes, rows.size());
    }
}
