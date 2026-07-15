package com.procureai.infra.seed;

import com.procureai.auth.AppUser;
import com.procureai.auth.Role;
import com.procureai.auth.UserRepository;
import com.procureai.inventory.InventoryItem;
import com.procureai.inventory.InventoryRepository;
import com.procureai.quotation.Quotation;
import com.procureai.quotation.QuotationRepository;
import com.procureai.vendor.ComplianceStatus;
import com.procureai.vendor.Vendor;
import com.procureai.vendor.VendorPerformanceHistory;
import com.procureai.vendor.VendorPerformanceRepository;
import com.procureai.vendor.VendorRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoDataSeeder {

    @Bean
    CommandLineRunner seedDemoData(
            UserRepository userRepository,
            VendorRepository vendorRepository,
            VendorPerformanceRepository performanceRepository,
            InventoryRepository inventoryRepository,
            QuotationRepository quotationRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            seedUsers(userRepository, passwordEncoder);
            List<Vendor> vendors = seedVendors(vendorRepository);
            seedPerformance(performanceRepository, vendors);
            seedInventory(inventoryRepository);
            seedQuotations(quotationRepository, vendors);
        };
    }

    private void seedUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.save(user("admin@procureai.local", "Admin User", Set.of(Role.ADMIN), passwordEncoder));
        userRepository.save(user("officer@procureai.local", "Procurement Officer", Set.of(Role.PROCUREMENT_OFFICER), passwordEncoder));
        userRepository.save(user("approver1@procureai.local", "Level 1 Approver", Set.of(Role.APPROVER_L1), passwordEncoder));
        userRepository.save(user("approver2@procureai.local", "Level 2 Approver", Set.of(Role.APPROVER_L2), passwordEncoder));
        userRepository.save(user("vendor-manager@procureai.local", "Vendor Manager", Set.of(Role.VENDOR_MANAGER), passwordEncoder));
    }

    private AppUser user(String email, String fullName, Set<Role> roles, PasswordEncoder passwordEncoder) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRoles(roles);
        return user;
    }

    private List<Vendor> seedVendors(VendorRepository vendorRepository) {
        if (vendorRepository.count() > 0) {
            return vendorRepository.findAll();
        }
        Vendor apex = vendor("Apex Components", "Electronics", "4.70", ComplianceStatus.COMPLIANT, 18);
        Vendor northstar = vendor("Northstar Logistics", "Logistics", "4.20", ComplianceStatus.PENDING_REVIEW, 34);
        Vendor prism = vendor("Prism Industrial Supply", "MRO", "4.55", ComplianceStatus.COMPLIANT, 22);
        return vendorRepository.saveAll(List.of(apex, northstar, prism));
    }

    private Vendor vendor(String name, String category, String rating, ComplianceStatus status, int riskScore) {
        Vendor vendor = new Vendor();
        vendor.setName(name);
        vendor.setCategory(category);
        vendor.setRating(new BigDecimal(rating));
        vendor.setComplianceStatus(status);
        vendor.setRiskScore(riskScore);
        return vendor;
    }

    private void seedPerformance(VendorPerformanceRepository performanceRepository, List<Vendor> vendors) {
        if (performanceRepository.count() > 0 || vendors.isEmpty()) {
            return;
        }
        long poId = 1000L;
        for (Vendor vendor : vendors) {
            performanceRepository.save(performance(vendor, poId++, "96.50", "4.70", 0));
            performanceRepository.save(performance(vendor, poId++, "91.00", "4.30", vendor.getRiskScore() > 30 ? 1 : 0));
        }
    }

    private VendorPerformanceHistory performance(
            Vendor vendor,
            Long purchaseOrderId,
            String onTimeDeliveryPct,
            String qualityScore,
            int disputeCount
    ) {
        VendorPerformanceHistory history = new VendorPerformanceHistory();
        history.setVendor(vendor);
        history.setPurchaseOrderId(purchaseOrderId);
        history.setOnTimeDeliveryPct(new BigDecimal(onTimeDeliveryPct));
        history.setQualityScore(new BigDecimal(qualityScore));
        history.setDisputeCount(disputeCount);
        return history;
    }

    private void seedInventory(InventoryRepository inventoryRepository) {
        if (inventoryRepository.count() > 0) {
            return;
        }
        inventoryRepository.save(item("ELEC-CTRL-01", "Controller board", 143, "pcs"));
        inventoryRepository.save(item("MRO-BEAR-11", "Industrial bearing", 38, "pcs"));
        inventoryRepository.save(item("LOG-PALLET-STD", "Standard pallet slot", 0, "slots"));
    }

    private InventoryItem item(String sku, String name, int stockLevel, String unit) {
        InventoryItem item = new InventoryItem();
        item.setSku(sku);
        item.setName(name);
        item.setStockLevel(stockLevel);
        item.setUnit(unit);
        return item;
    }

    private void seedQuotations(QuotationRepository quotationRepository, List<Vendor> vendors) {
        if (quotationRepository.count() > 0 || vendors.size() < 3) {
            return;
        }
        quotationRepository.save(quotation(vendors.get(0), "RFQ-2026-001", "{\"sku\":\"ELEC-CTRL-01\",\"qty\":100}", "128000.00", 18));
        quotationRepository.save(quotation(vendors.get(1), "RFQ-2026-001", "{\"sku\":\"ELEC-CTRL-01\",\"qty\":100}", "119500.00", 25));
        quotationRepository.save(quotation(vendors.get(2), "RFQ-2026-001", "{\"sku\":\"ELEC-CTRL-01\",\"qty\":100}", "132250.00", 14));
    }

    private Quotation quotation(Vendor vendor, String rfqId, String itemsJson, String totalCost, int deliveryDays) {
        Quotation quotation = new Quotation();
        quotation.setVendor(vendor);
        quotation.setRfqId(rfqId);
        quotation.setItemsJson(itemsJson);
        quotation.setTotalCost(new BigDecimal(totalCost));
        quotation.setDeliveryDays(deliveryDays);
        return quotation;
    }
}
