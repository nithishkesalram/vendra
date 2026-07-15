package com.procureai.contract;

import com.procureai.contract.dto.ContractRequest;
import com.procureai.contract.dto.ContractResponse;
import com.procureai.contract.dto.ContractRiskResponse;
import com.procureai.contract.dto.DocumentUploadResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/contracts")
@PreAuthorize("hasAnyRole('ADMIN','VENDOR_MANAGER','PROCUREMENT_OFFICER','APPROVER_L1','APPROVER_L2')")
public class ContractController {

    private final ContractService contractService;
    private final ContractRiskService contractRiskService;

    public ContractController(ContractService contractService, ContractRiskService contractRiskService) {
        this.contractService = contractService;
        this.contractRiskService = contractRiskService;
    }

    @PostMapping
    public ContractResponse create(@Valid @RequestBody ContractRequest request) {
        return contractService.create(request);
    }

    @GetMapping("/{id}")
    public ContractResponse get(@PathVariable Long id) {
        return contractService.get(id);
    }

    @GetMapping
    public List<ContractResponse> list(@RequestParam Long vendorId) {
        return contractService.listForVendor(vendorId);
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentUploadResponse upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return contractService.uploadDocument(id, file);
    }

    @PostMapping("/{id}/risk-analysis")
    public ContractRiskResponse analyze(@PathVariable Long id) {
        return contractRiskService.analyze(id);
    }
}
