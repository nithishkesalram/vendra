package com.procureai.ai.chat.dto;

import java.util.List;

public record ChatResponse(
        String reply,
        List<ToolCallRecord> toolCalls,
        List<Citation> citations
) {
}
