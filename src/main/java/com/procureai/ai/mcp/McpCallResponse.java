package com.procureai.ai.mcp;

public record McpCallResponse(
        String toolName,
        Object result
) {
}
