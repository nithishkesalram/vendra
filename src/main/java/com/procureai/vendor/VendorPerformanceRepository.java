package com.procureai.vendor;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorPerformanceRepository extends JpaRepository<VendorPerformanceHistory, Long> {

    List<VendorPerformanceHistory> findByVendorId(Long vendorId);
}
