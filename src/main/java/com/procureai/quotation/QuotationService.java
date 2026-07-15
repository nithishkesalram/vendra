package com.procureai.quotation;

import com.procureai.common.exception.NotFoundException;
import com.procureai.audit.AuditLogged;
import com.procureai.quotation.dto.QuotationComparisonResponse;
import com.procureai.quotation.dto.QuotationRequest;
import com.procureai.quotation.dto.QuotationResponse;
import com.procureai.vendor.Vendor;
import com.procureai.vendor.VendorService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final VendorService vendorService;

    public QuotationService(QuotationRepository quotationRepository, VendorService vendorService) {
        this.quotationRepository = quotationRepository;
        this.vendorService = vendorService;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    @AuditLogged(entityType = "QUOTATION", action = "CREATE")
    public QuotationResponse create(QuotationRequest request) {
        Vendor vendor = vendorService.findEntity(request.vendorId());
        Quotation quotation = new Quotation();
        quotation.setVendor(vendor);
        quotation.setRfqId(request.rfqId());
        quotation.setItemsJson(request.itemsJson());
        quotation.setTotalCost(request.totalCost());
        quotation.setDeliveryDays(request.deliveryDays());
        return toResponse(quotationRepository.save(quotation));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER','APPROVER_L1','APPROVER_L2')")
    public QuotationResponse get(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER','APPROVER_L1','APPROVER_L2')")
    public List<QuotationResponse> listByRfq(String rfqId) {
        return quotationRepository.findByRfqIdOrderBySubmittedAtDesc(rfqId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER','APPROVER_L1','APPROVER_L2')")
    public List<QuotationComparisonResponse> compare(String rfqId) {
        List<Quotation> quotations = quotationRepository.findByRfqIdOrderBySubmittedAtDesc(rfqId);
        if (quotations.isEmpty()) {
            throw new NotFoundException("No quotations found for RFQ %s".formatted(rfqId));
        }
        BigDecimal lowestCost = quotations.stream()
                .map(Quotation::getTotalCost)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ONE);
        int fastestDelivery = quotations.stream()
                .mapToInt(Quotation::getDeliveryDays)
                .min()
                .orElse(1);

        return quotations.stream()
                .map(quotation -> score(quotation, lowestCost, fastestDelivery))
                .peek(scored -> findEntity(scored.quotationId()).setAiScore(scored.aiScore()))
                .sorted(Comparator.comparing(QuotationComparisonResponse::aiScore).reversed())
                .toList();
    }

    public Quotation findEntity(Long id) {
        return quotationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Quotation %d not found".formatted(id)));
    }

    private QuotationComparisonResponse score(Quotation quotation, BigDecimal lowestCost, int fastestDelivery) {
        BigDecimal costScore = lowestCost
                .divide(quotation.getTotalCost(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(50));
        BigDecimal deliveryScore = BigDecimal.valueOf(fastestDelivery)
                .divide(BigDecimal.valueOf(quotation.getDeliveryDays()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(25));
        BigDecimal qualityScore = quotation.getVendor().getRating()
                .divide(BigDecimal.valueOf(5), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(20));
        BigDecimal riskScore = BigDecimal.valueOf(100 - quotation.getVendor().getRiskScore())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(5));
        BigDecimal score = costScore.add(deliveryScore).add(qualityScore).add(riskScore)
                .setScale(2, RoundingMode.HALF_UP);
        quotation.setAiScore(score);
        String rationale = "Weighted score: 50% cost, 25% delivery, 20% vendor rating, 5% risk posture.";
        return new QuotationComparisonResponse(
                quotation.getId(),
                quotation.getVendor().getId(),
                quotation.getVendor().getName(),
                quotation.getRfqId(),
                quotation.getTotalCost(),
                quotation.getDeliveryDays(),
                score,
                rationale
        );
    }

    private QuotationResponse toResponse(Quotation quotation) {
        return new QuotationResponse(
                quotation.getId(),
                quotation.getVendor().getId(),
                quotation.getVendor().getName(),
                quotation.getRfqId(),
                quotation.getItemsJson(),
                quotation.getTotalCost(),
                quotation.getDeliveryDays(),
                quotation.getSubmittedAt(),
                quotation.getAiScore()
        );
    }
}
