package com.procureai.quotation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    List<Quotation> findByRfqIdOrderBySubmittedAtDesc(String rfqId);
}
