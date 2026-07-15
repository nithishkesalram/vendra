package com.procureai.ai.chat.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ChatRequest(
        @NotBlank String message,
        Long vendorId,
        List<ChatMessage> history
) {
}
