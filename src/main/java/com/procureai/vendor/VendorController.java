package com.procureai.vendor;

import com.procureai.common.dto.PageResponse;
import com.procureai.vendor.dto.VendorRequest;
import com.procureai.vendor.dto.VendorResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vendors")
@PreAuthorize("hasAnyRole('ADMIN','VENDOR_MANAGER','PROCUREMENT_OFFICER','APPROVER_L1','APPROVER_L2')")
public class VendorController {

    private final VendorService vendorService;
    private final VendorPerformanceService vendorPerformanceService;

    public VendorController(VendorService vendorService, VendorPerformanceService vendorPerformanceService) {
        this.vendorService = vendorService;
        this.vendorPerformanceService = vendorPerformanceService;
    }

    @GetMapping("/{id}")
    public VendorResponse get(@PathVariable Long id) {
        return vendorService.get(id);
    }

    @GetMapping
    public PageResponse<VendorResponse> search(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) ComplianceStatus complianceStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return vendorService.search(category, minRating, complianceStatus, page, size);
    }

    @GetMapping("/{id}/performance")
    public com.procureai.vendor.dto.VendorPerformanceResponse performance(@PathVariable Long id) {
        return vendorPerformanceService.getPerformance(id);
    }

    @PostMapping
    public VendorResponse create(@Valid @RequestBody VendorRequest request) {
        return vendorService.create(request);
    }

    @PutMapping("/{id}")
    public VendorResponse update(@PathVariable Long id, @Valid @RequestBody VendorRequest request) {
        return vendorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        vendorService.delete(id);
    }
}
