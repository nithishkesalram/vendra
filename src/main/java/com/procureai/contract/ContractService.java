package com.procureai.contract;

import com.procureai.ai.rag.DocumentChunk;
import com.procureai.ai.rag.DocumentChunkRepository;
import com.procureai.ai.rag.DocumentTextExtractor;
import com.procureai.ai.rag.SourceType;
import com.procureai.ai.rag.TextChunker;
import com.procureai.audit.AuditLogged;
import com.procureai.common.exception.NotFoundException;
import com.procureai.common.security.SecurityUtils;
import com.procureai.contract.dto.ContractRequest;
import com.procureai.contract.dto.ContractResponse;
import com.procureai.contract.dto.DocumentUploadResponse;
import com.procureai.infra.kafka.DocumentUploadedEvent;
import com.procureai.infra.kafka.ProcurementEventPublisher;
import com.procureai.vendor.Vendor;
import com.procureai.vendor.VendorService;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final VendorService vendorService;
    private final DocumentTextExtractor textExtractor;
    private final TextChunker textChunker;
    private final DocumentChunkRepository documentChunkRepository;
    private final ProcurementEventPublisher eventPublisher;

    public ContractService(
            ContractRepository contractRepository,
            VendorService vendorService,
            DocumentTextExtractor textExtractor,
            TextChunker textChunker,
            DocumentChunkRepository documentChunkRepository,
            ProcurementEventPublisher eventPublisher
    ) {
        this.contractRepository = contractRepository;
        this.vendorService = vendorService;
        this.textExtractor = textExtractor;
        this.textChunker = textChunker;
        this.documentChunkRepository = documentChunkRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR_MANAGER','PROCUREMENT_OFFICER')")
    @AuditLogged(entityType = "CONTRACT", action = "CREATE")
    public ContractResponse create(ContractRequest request) {
        Vendor vendor = vendorService.findEntity(request.vendorId());
        Contract contract = new Contract();
        contract.setVendor(vendor);
        contract.setDocumentUrl(request.documentUrl());
        contract.setExpiryDate(request.expiryDate());
        return toResponse(contractRepository.save(contract));
    }

    @Transactional(readOnly = true)
    public ContractResponse get(Long id) {
        return toResponse(findEntity(id));
    }

    @Transactional(readOnly = true)
    public List<ContractResponse> listForVendor(Long vendorId) {
        return contractRepository.findByVendorId(vendorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','VENDOR_MANAGER','PROCUREMENT_OFFICER')")
    @AuditLogged(entityType = "CONTRACT", action = "UPLOAD_DOCUMENT")
    public DocumentUploadResponse uploadDocument(Long contractId, MultipartFile file) {
        Contract contract = findEntity(contractId);
        String text = extractText(file);
        List<String> chunks = textChunker.chunk(text);
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setSourceDocId(contract.getId());
            chunk.setVendorId(contract.getVendor().getId());
            chunk.setSourceType(SourceType.CONTRACT);
            chunk.setContent(chunks.get(i));
            chunk.setMetadataJson("{\"chunkIndex\":%d,\"filename\":\"%s\"}".formatted(i, safeFilename(file)));
            chunk.setEmbeddingJson("[]");
            chunk.setPageNumber(i + 1);
            documentChunkRepository.save(chunk);
        }
        contract.setDocumentUrl("uploaded:" + safeFilename(file));
        contract.setExtractedClausesJson("{\"chunksCreated\":" + chunks.size() + "}");
        eventPublisher.documentUploaded(new DocumentUploadedEvent(
                UUID.randomUUID(),
                contract.getId(),
                contract.getVendor().getId(),
                chunks.size(),
                SecurityUtils.currentActor(),
                Instant.now()
        ));
        return new DocumentUploadResponse(contract.getId(), contract.getVendor().getId(), chunks.size(), contract.getDocumentUrl());
    }

    public Contract findEntity(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Contract %d not found".formatted(id)));
    }

    private String extractText(MultipartFile file) {
        try {
            String text = textExtractor.extract(file);
            if (text.isBlank()) {
                throw new IllegalArgumentException("Uploaded document has no extractable text");
            }
            return text;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not read uploaded document", ex);
        }
    }

    private String safeFilename(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            return "document";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private ContractResponse toResponse(Contract contract) {
        return new ContractResponse(
                contract.getId(),
                contract.getVendor().getId(),
                contract.getVendor().getName(),
                contract.getDocumentUrl(),
                contract.getExtractedClausesJson(),
                contract.getRiskLevel(),
                contract.getExpiryDate()
        );
    }
}
