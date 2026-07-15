package com.procureai.inventory;

import com.procureai.common.exception.NotFoundException;
import com.procureai.inventory.dto.InventoryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','PROCUREMENT_OFFICER','APPROVER_L1','APPROVER_L2')")
    public InventoryResponse checkInventory(String sku) {
        InventoryItem item = inventoryRepository.findBySkuIgnoreCase(sku)
                .orElseThrow(() -> new NotFoundException("SKU %s not found".formatted(sku)));
        return new InventoryResponse(
                item.getSku(),
                item.getName(),
                item.getStockLevel(),
                item.getUnit(),
                item.getStockLevel() > 0
        );
    }
}
