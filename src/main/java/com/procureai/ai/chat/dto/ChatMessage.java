package com.procureai.ai.chat.dto;

public record ChatMessage(
        String role,
        String content
) {
}
