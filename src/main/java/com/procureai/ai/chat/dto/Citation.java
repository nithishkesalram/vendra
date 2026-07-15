package com.procureai.ai.chat.dto;

public record Citation(
        Long chunkId,
        Long sourceDocId,
        String sourceType,
        String excerpt
) {
}
