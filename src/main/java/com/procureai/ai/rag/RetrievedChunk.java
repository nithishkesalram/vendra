package com.procureai.ai.rag;

public record RetrievedChunk(
        Long chunkId,
        Long sourceDocId,
        Long vendorId,
        SourceType sourceType,
        String content,
        double score,
        String confidence,
        String metadataJson
) {
    public RetrievedChunk(
            Long chunkId,
            Long sourceDocId,
            Long vendorId,
            SourceType sourceType,
            String content,
            double score,
            String metadataJson
    ) {
        this(
                chunkId,
                sourceDocId,
                vendorId,
                sourceType,
                content,
                score,
                toConfidence(score),
                metadataJson
        );
    }

    private static String toConfidence(double score) {
        if (score >= 0.70) {
            return "HIGH";
        }
        if (score >= 0.35) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
