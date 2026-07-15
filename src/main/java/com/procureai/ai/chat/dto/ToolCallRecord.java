package com.procureai.ai.chat.dto;

import java.util.Map;

public record ToolCallRecord(
        String name,
        Map<String, Object> arguments
) {
}
