package com.procureai.ai.rag;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetrievalService {

    private final DocumentChunkRepository documentChunkRepository;

    public RetrievalService(DocumentChunkRepository documentChunkRepository) {
        this.documentChunkRepository = documentChunkRepository;
    }

    @Transactional(readOnly = true)
    public List<RetrievedChunk> retrieveRelevantChunks(String query, Long vendorId, int topK) {
        List<DocumentChunk> chunks = vendorId == null
                ? documentChunkRepository.findAll()
                : documentChunkRepository.findByVendorId(vendorId);
        Set<String> queryTerms = tokenize(query);
        return chunks.stream()
                .map(chunk -> toRetrievedChunk(chunk, queryTerms))
                .filter(chunk -> chunk.score() > 0)
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(topK)
                .toList();
    }

    private RetrievedChunk toRetrievedChunk(DocumentChunk chunk, Set<String> queryTerms) {
        Set<String> contentTerms = tokenize(chunk.getContent());
        long matches = queryTerms.stream().filter(contentTerms::contains).count();
        double score = queryTerms.isEmpty() ? 0 : (double) matches / queryTerms.size();
        return new RetrievedChunk(
                chunk.getId(),
                chunk.getSourceDocId(),
                chunk.getVendorId(),
                chunk.getSourceType(),
                chunk.getContent(),
                score,
                chunk.getMetadataJson()
        );
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(token -> token.length() > 2)
                .collect(Collectors.toSet());
    }
}
