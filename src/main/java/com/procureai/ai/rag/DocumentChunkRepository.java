package com.procureai.ai.rag;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByVendorId(Long vendorId);

    List<DocumentChunk> findBySourceDocId(Long sourceDocId);
}
