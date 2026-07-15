package com.procureai.vendor;

import com.procureai.common.dto.PageResponse;
import com.procureai.common.exception.NotFoundException;
import com.procureai.audit.AuditLogged;
import com.procureai.vendor.dto.VendorRequest;
import com.procureai.vendor.dto.VendorResponse;
import java.math.BigDecimal;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorMapper vendorMapper;

    public VendorService(VendorRepository vendorRepository, VendorMapper vendorMapper) {
        this.vendorRepository = vendorRepository;
        this.vendorMapper = vendorMapper;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "vendors", key = "'id:' + #id")
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR_MANAGER','PROCUREMENT_OFFICER','APPROVER_L1','APPROVER_L2')")
    public VendorResponse get(Long id) {
        return vendorMapper.toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "vendors", key = "'search:' + #category + ':' + #minRating + ':' + #status + ':' + #page + ':' + #size")
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR_MANAGER','PROCUREMENT_OFFICER','APPROVER_L1','APPROVER_L2')")
    public PageResponse<VendorResponse> search(
            String category,
            BigDecimal minRating,
            ComplianceStatus status,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Vendor> vendors = vendorRepository.findAll(specification(category, minRating, status), pageRequest);
        return new PageResponse<>(
                vendors.map(vendorMapper::toResponse).toList(),
                vendors.getNumber(),
                vendors.getSize(),
                vendors.getTotalElements(),
                vendors.getTotalPages()
        );
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR_MANAGER')")
    @CacheEvict(value = "vendors", allEntries = true)
    @AuditLogged(entityType = "VENDOR", action = "CREATE")
    public VendorResponse create(VendorRequest request) {
        Vendor vendor = vendorMapper.toEntity(request);
        if (vendor.getRiskScore() == null) {
            vendor.setRiskScore(0);
        }
        return vendorMapper.toResponse(vendorRepository.save(vendor));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR_MANAGER')")
    @CacheEvict(value = "vendors", allEntries = true)
    @AuditLogged(entityType = "VENDOR", action = "UPDATE")
    public VendorResponse update(Long id, VendorRequest request) {
        Vendor vendor = findEntity(id);
        vendorMapper.updateEntity(request, vendor);
        return vendorMapper.toResponse(vendorRepository.save(vendor));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "vendors", allEntries = true)
    @AuditLogged(entityType = "VENDOR", action = "DELETE")
    public void delete(Long id) {
        Vendor vendor = findEntity(id);
        vendorRepository.delete(vendor);
    }

    public Vendor findEntity(Long id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Vendor %d not found".formatted(id)));
    }

    private Specification<Vendor> specification(String category, BigDecimal minRating, ComplianceStatus status) {
        Specification<Vendor> spec = Specification.where(null);
        if (category != null && !category.isBlank()) {
            spec = spec.and((root, query, builder) ->
                    builder.equal(builder.lower(root.get("category")), category.toLowerCase()));
        }
        if (minRating != null) {
            spec = spec.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("rating"), minRating));
        }
        if (status != null) {
            spec = spec.and((root, query, builder) -> builder.equal(root.get("complianceStatus"), status));
        }
        return spec;
    }
}
