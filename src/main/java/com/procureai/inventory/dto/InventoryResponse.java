package com.procureai.inventory.dto;

public record InventoryResponse(
        String sku,
        String name,
        Integer stockLevel,
        String unit,
        boolean available
) {
}
