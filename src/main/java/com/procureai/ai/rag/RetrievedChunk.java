package com.procureai.ai.rag;

public record RetrievedChunk(
        Long chunkId,
        Long sourceDocId,
        Long vendorId,
        SourceType sourceType,
        String content,
        double score,
        String metadataJson
) {
}
