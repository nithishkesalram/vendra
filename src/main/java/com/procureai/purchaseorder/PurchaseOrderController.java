package com.procureai.purchaseorder;

import com.procureai.common.dto.PageResponse;
import com.procureai.purchaseorder.dto.PurchaseOrderActionRequest;
import com.procureai.purchaseorder.dto.PurchaseOrderRequest;
import com.procureai.purchaseorder.dto.PurchaseOrderResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/purchase-orders")
@PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER','APPROVER_L1','APPROVER_L2')")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    public PurchaseOrderResponse create(@Valid @RequestBody PurchaseOrderRequest request) {
        return purchaseOrderService.create(request);
    }

    @PostMapping("/{id}/submit")
    public PurchaseOrderResponse submit(@PathVariable Long id) {
        return purchaseOrderService.submit(id);
    }

    @PostMapping("/{id}/approve")
    public PurchaseOrderResponse approve(
            @PathVariable Long id,
            @RequestBody(required = false) PurchaseOrderActionRequest request
    ) {
        return purchaseOrderService.approve(id, request);
    }

    @PostMapping("/{id}/reject")
    public PurchaseOrderResponse reject(
            @PathVariable Long id,
            @RequestBody(required = false) PurchaseOrderActionRequest request
    ) {
        return purchaseOrderService.reject(id, request);
    }

    @GetMapping("/{id}")
    public PurchaseOrderResponse get(@PathVariable Long id) {
        return purchaseOrderService.get(id);
    }

    @GetMapping
    public PageResponse<PurchaseOrderResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return purchaseOrderService.list(page, size);
    }
}
