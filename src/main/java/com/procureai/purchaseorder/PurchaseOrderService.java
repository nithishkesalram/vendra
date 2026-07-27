package com.procureai.purchaseorder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureai.auth.Role;
import com.procureai.audit.AuditLogged;
import com.procureai.common.dto.PageResponse;
import com.procureai.common.exception.NotFoundException;
import com.procureai.common.security.SecurityUtils;
import com.procureai.infra.kafka.PoCreatedEvent;
import com.procureai.infra.kafka.PoStatusChangedEvent;
import com.procureai.infra.kafka.ProcurementEventPublisher;
import com.procureai.purchaseorder.dto.ApprovalStepResponse;
import com.procureai.purchaseorder.dto.PurchaseOrderActionRequest;
import com.procureai.purchaseorder.dto.PurchaseOrderRequest;
import com.procureai.purchaseorder.dto.PurchaseOrderResponse;
import com.procureai.vendor.Vendor;
import com.procureai.vendor.VendorService;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final VendorService vendorService;
    private final ProcurementEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            VendorService vendorService,
            ProcurementEventPublisher eventPublisher,
            ObjectMapper objectMapper
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.vendorService = vendorService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    @AuditLogged(entityType = "PURCHASE_ORDER", action = "CREATE")
    public PurchaseOrderResponse create(PurchaseOrderRequest request) {
        Vendor vendor = vendorService.findEntity(request.vendorId());
        PurchaseOrder order = new PurchaseOrder();
        order.setVendor(vendor);
        order.setAmount(request.amount());
        order.setItemsJson(toItemsJson(request));
        order.setApproverChain(normalizeApproverChain(request.approverChain()));
        order.setCreatedBy(SecurityUtils.currentActor());
        PurchaseOrder saved = purchaseOrderRepository.save(order);
        eventPublisher.poCreated(new PoCreatedEvent(
                UUID.randomUUID(),
                saved.getId(),
                vendor.getId(),
                saved.getAmount(),
                SecurityUtils.currentActor(),
                Instant.now()
        ));
        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER')")
    @AuditLogged(entityType = "PURCHASE_ORDER", action = "SUBMIT")
    public PurchaseOrderResponse submit(Long id) {
        PurchaseOrder order = findEntity(id);
        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Only draft purchase orders can be submitted");
        }
        order.setStatus(PurchaseOrderStatus.PENDING_APPROVAL);
        order.getApprovalSteps().clear();
        for (String roleName : order.getApproverChain().split(">")) {
            ApprovalStep step = new ApprovalStep();
            step.setApproverRole(Role.valueOf(roleName.trim()));
            order.addApprovalStep(step);
        }
        return toResponse(purchaseOrderRepository.save(order));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER_L1','APPROVER_L2')")
    @AuditLogged(entityType = "PURCHASE_ORDER", action = "APPROVE")
    public PurchaseOrderResponse approve(Long id, PurchaseOrderActionRequest request) {
        PurchaseOrder order = findEntity(id);
        ApprovalStep step = nextPendingStep(order);
        authorizeApprover(step.getApproverRole());
        step.setStatus(ApprovalStepStatus.APPROVED);
        step.setActedAt(Instant.now());
        step.setComments(request == null ? null : request.comments());
        if (order.getApprovalSteps().stream().allMatch(candidate -> candidate.getStatus() == ApprovalStepStatus.APPROVED)) {
            transition(order, PurchaseOrderStatus.APPROVED);
        }
        return toResponse(purchaseOrderRepository.save(order));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','APPROVER_L1','APPROVER_L2')")
    @AuditLogged(entityType = "PURCHASE_ORDER", action = "REJECT")
    public PurchaseOrderResponse reject(Long id, PurchaseOrderActionRequest request) {
        PurchaseOrder order = findEntity(id);
        ApprovalStep step = nextPendingStep(order);
        authorizeApprover(step.getApproverRole());
        step.setStatus(ApprovalStepStatus.REJECTED);
        step.setActedAt(Instant.now());
        step.setComments(request == null ? null : request.comments());
        transition(order, PurchaseOrderStatus.REJECTED);
        return toResponse(purchaseOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER','APPROVER_L1','APPROVER_L2')")
    public PurchaseOrderResponse get(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER','APPROVER_L1','APPROVER_L2')")
    public PageResponse<PurchaseOrderResponse> list(int page, int size) {
        Page<PurchaseOrder> orders = purchaseOrderRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return new PageResponse<>(
                orders.map(this::toResponse).toList(),
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages()
        );
    }

    public PurchaseOrder findEntity(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Purchase order %d not found".formatted(id)));
    }

    private void transition(PurchaseOrder order, PurchaseOrderStatus newStatus) {
        PurchaseOrderStatus previous = order.getStatus();
        order.setStatus(newStatus);
        eventPublisher.poStatusChanged(new PoStatusChangedEvent(
                UUID.randomUUID(),
                order.getId(),
                previous.name(),
                newStatus.name(),
                SecurityUtils.currentActor(),
                Instant.now()
        ));
    }

    private ApprovalStep nextPendingStep(PurchaseOrder order) {
        if (order.getStatus() != PurchaseOrderStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Purchase order is not pending approval");
        }
        return order.getApprovalSteps().stream()
                .filter(step -> step.getStatus() == ApprovalStepStatus.PENDING)
                .min(Comparator.comparing(step -> step.getApproverRole().ordinal()))
                .orElseThrow(() -> new IllegalStateException("No pending approval step remains"));
    }

    private void authorizeApprover(Role requiredRole) {
        Set<String> roles = SecurityUtils.currentRoles();
        if (!roles.contains(Role.ADMIN.name()) && !roles.contains(requiredRole.name())) {
            throw new AccessDeniedException("Current user cannot approve this step");
        }
    }

    private String normalizeApproverChain(String approverChain) {
        if (approverChain == null || approverChain.isBlank()) {
            return Role.APPROVER_L1.name() + ">" + Role.APPROVER_L2.name();
        }
        for (String roleName : approverChain.split(">")) {
            Role.valueOf(roleName.trim());
        }
        return approverChain;
    }

    private String toItemsJson(PurchaseOrderRequest request) {
        if (request.lineItems() != null && !request.lineItems().isEmpty()) {
            try {
                return objectMapper.writeValueAsString(request.lineItems());
            } catch (JsonProcessingException exception) {
                throw new IllegalArgumentException("Line items could not be converted to JSON", exception);
            }
        }
        if (request.itemsJson() != null && !request.itemsJson().isBlank()) {
            return request.itemsJson();
        }
        throw new IllegalArgumentException("At least one line item is required");
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder order) {
        List<ApprovalStepResponse> steps = order.getApprovalSteps().stream()
                .sorted(Comparator.comparing(step -> step.getApproverRole().ordinal()))
                .map(step -> new ApprovalStepResponse(
                        step.getId(),
                        step.getApproverRole(),
                        step.getStatus(),
                        step.getActedAt(),
                        step.getComments()
                ))
                .toList();
        return new PurchaseOrderResponse(
                order.getId(),
                order.getVendor().getId(),
                order.getVendor().getName(),
                order.getStatus(),
                order.getApproverChain(),
                order.getCreatedBy(),
                order.getAmount(),
                order.getItemsJson(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                steps
        );
    }
}
