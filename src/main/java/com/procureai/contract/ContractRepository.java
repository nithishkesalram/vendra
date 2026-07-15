package com.procureai.contract;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByVendorId(Long vendorId);
}
