package com.procureai.ai.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.procureai.contract.ContractRiskService;
import com.procureai.inventory.InventoryService;
import com.procureai.purchaseorder.PurchaseOrderService;
import com.procureai.purchaseorder.dto.PurchaseOrderRequest;
import com.procureai.quotation.QuotationService;
import com.procureai.vendor.ComplianceStatus;
import com.procureai.vendor.VendorPerformanceService;
import com.procureai.vendor.VendorService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class McpToolRegistry {

    private final VendorService vendorService;
    private final VendorPerformanceService vendorPerformanceService;
    private final QuotationService quotationService;
    private final InventoryService inventoryService;
    private final PurchaseOrderService purchaseOrderService;
    private final ContractRiskService contractRiskService;
    private final ObjectMapper objectMapper;

    public McpToolRegistry(
            VendorService vendorService,
            VendorPerformanceService vendorPerformanceService,
            QuotationService quotationService,
            InventoryService inventoryService,
            PurchaseOrderService purchaseOrderService,
            ContractRiskService contractRiskService,
            ObjectMapper objectMapper
    ) {
        this.vendorService = vendorService;
        this.vendorPerformanceService = vendorPerformanceService;
        this.quotationService = quotationService;
        this.inventoryService = inventoryService;
        this.purchaseOrderService = purchaseOrderService;
        this.contractRiskService = contractRiskService;
        this.objectMapper = objectMapper;
    }

    public List<McpToolDescriptor> listTools() {
        return List.of(
                tool("search_vendors", "Search/filter vendors by category, rating, and compliance.", Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "category", Map.of("type", "string"),
                                "minRating", Map.of("type", "number"),
                                "complianceStatus", Map.of("type", "string"),
                                "page", Map.of("type", "integer"),
                                "size", Map.of("type", "integer")
                        )
                )),
                tool("get_vendor_performance", "Return historical delivery, quality, and dispute metrics.", Map.of(
                        "type", "object",
                        "required", List.of("vendorId"),
                        "properties", Map.of("vendorId", Map.of("type", "integer"))
                )),
                tool("compare_quotations", "Score and rank quotations for an RFQ.", Map.of(
                        "type", "object",
                        "required", List.of("rfqId"),
                        "properties", Map.of("rfqId", Map.of("type", "string"))
                )),
                tool("check_inventory", "Check current stock level for an item SKU.", Map.of(
                        "type", "object",
                        "required", List.of("sku"),
                        "properties", Map.of("sku", Map.of("type", "string"))
                )),
                tool("create_purchase_order", "Draft a purchase order. RBAC is enforced by PurchaseOrderService.", Map.of(
                        "type", "object",
                        "required", List.of("vendorId", "amount", "itemsJson"),
                        "properties", Map.of(
                                "vendorId", Map.of("type", "integer"),
                                "amount", Map.of("type", "number"),
                                "itemsJson", Map.of("type", "string"),
                                "approverChain", Map.of("type", "string")
                        )
                )),
                tool("get_approval_status", "Fetch a purchase order and its approval chain.", Map.of(
                        "type", "object",
                        "required", List.of("purchaseOrderId"),
                        "properties", Map.of("purchaseOrderId", Map.of("type", "integer"))
                )),
                tool("analyze_contract_risk", "Analyze a contract for risky clauses with citations.", Map.of(
                        "type", "object",
                        "required", List.of("contractId"),
                        "properties", Map.of("contractId", Map.of("type", "integer"))
                ))
        );
    }

    public Object call(String toolName, Map<String, Object> arguments) {
        Map<String, Object> args = arguments == null ? Map.of() : arguments;
        return switch (toolName) {
            case "search_vendors" -> vendorService.search(
                    stringArg(args, "category"),
                    decimalArg(args, "minRating"),
                    enumArg(args, "complianceStatus"),
                    intArg(args, "page", 0),
                    intArg(args, "size", 20)
            );
            case "get_vendor_performance" -> vendorPerformanceService.getPerformance(longArg(args, "vendorId"));
            case "compare_quotations" -> quotationService.compare(requiredStringArg(args, "rfqId"));
            case "check_inventory" -> inventoryService.checkInventory(requiredStringArg(args, "sku"));
            case "create_purchase_order" -> purchaseOrderService.create(objectMapper.convertValue(args, PurchaseOrderRequest.class));
            case "get_approval_status" -> purchaseOrderService.get(longArg(args, "purchaseOrderId"));
            case "analyze_contract_risk" -> contractRiskService.analyze(longArg(args, "contractId"));
            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };
    }

    private McpToolDescriptor tool(String name, String description, Map<String, Object> inputSchema) {
        return new McpToolDescriptor(name, description, inputSchema);
    }

    private String stringArg(Map<String, Object> args, String name) {
        Object value = args.get(name);
        return value == null ? null : value.toString();
    }

    private String requiredStringArg(Map<String, Object> args, String name) {
        String value = stringArg(args, name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private Long longArg(Map<String, Object> args, String name) {
        Object value = args.get(name);
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private int intArg(Map<String, Object> args, String name, int defaultValue) {
        Object value = args.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private BigDecimal decimalArg(Map<String, Object> args, String name) {
        Object value = args.get(name);
        if (value == null) {
            return null;
        }
        return new BigDecimal(value.toString());
    }

    private ComplianceStatus enumArg(Map<String, Object> args, String name) {
        Object value = args.get(name);
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return ComplianceStatus.valueOf(value.toString());
    }
}
