package com.procureai.quotation;

import com.procureai.quotation.dto.QuotationComparisonResponse;
import com.procureai.quotation.dto.QuotationRequest;
import com.procureai.quotation.dto.QuotationResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quotations")
@PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER','APPROVER_L1','APPROVER_L2')")
public class QuotationController {

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @PostMapping
    public QuotationResponse create(@Valid @RequestBody QuotationRequest request) {
        return quotationService.create(request);
    }

    @GetMapping("/{id}")
    public QuotationResponse get(@PathVariable Long id) {
        return quotationService.get(id);
    }

    @GetMapping
    public List<QuotationResponse> list(@RequestParam String rfqId) {
        return quotationService.listByRfq(rfqId);
    }

    @PostMapping("/rfq/{rfqId}/compare")
    public List<QuotationComparisonResponse> compare(@PathVariable String rfqId) {
        return quotationService.compare(rfqId);
    }
}
