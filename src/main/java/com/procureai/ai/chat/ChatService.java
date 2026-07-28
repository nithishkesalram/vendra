package com.procureai.ai.chat;

import com.procureai.ai.chat.dto.ChatRequest;
import com.procureai.ai.chat.dto.ChatResponse;
import com.procureai.ai.chat.dto.Citation;
import com.procureai.ai.chat.dto.ToolCallRecord;
import com.procureai.ai.llm.LlmClient;
import com.procureai.ai.rag.RetrievalService;
import com.procureai.ai.rag.RetrievedChunk;
import com.procureai.common.security.SecurityUtils;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final RetrievalService retrievalService;
    private final InMemoryRateLimiter rateLimiter;
    private final LlmClient llmClient;

    public ChatService(RetrievalService retrievalService, InMemoryRateLimiter rateLimiter, LlmClient llmClient) {
        this.retrievalService = retrievalService;
        this.rateLimiter = rateLimiter;
        this.llmClient = llmClient;
    }

    @Transactional(readOnly = true)
    public ChatResponse chat(ChatRequest request) {
        rateLimiter.assertAllowed(SecurityUtils.currentActor());
        String normalized = request.message().toLowerCase(Locale.ROOT);
        if (mentionsContracts(normalized)) {
            List<RetrievedChunk> chunks = retrievalService.retrieveRelevantChunks(request.message(), request.vendorId(), 4);
            if (chunks.isEmpty()) {
                return new ChatResponse(
                        "I could not find matching contract or policy context for your query. Upload a contract document in the Contracts workspace and try again.",
                        List.of(),
                        List.of()
                );
            }

            String synthesizedReply = llmClient.complete(
                    ragSystemPrompt(),
                    ragUserPrompt(request.message(), chunks)
            );

            List<Citation> citations = chunks.stream()
                    .map(chunk -> new Citation(
                            chunk.chunkId(),
                            chunk.sourceDocId(),
                            "%s [%s]".formatted(chunk.sourceType().name(), chunk.confidence()),
                            excerpt(chunk.content())
                    ))
                    .toList();

            return new ChatResponse(
                    synthesizedReply,
                    List.of(),
                    citations
            );
        }
        if (normalized.contains("stock") || normalized.contains("inventory")) {
            return new ChatResponse(
                    "This looks like an inventory lookup. Use the check_inventory tool with a SKU to get current stock.",
                    List.of(new ToolCallRecord("check_inventory", Map.of("sku", "SKU-EXAMPLE"))),
                    List.of()
            );
        }
        if (normalized.contains("purchase order") || normalized.contains(" po ")) {
            return new ChatResponse(
                    "I can help draft a purchase order when you provide vendorId, amount, and lineItems. The backend converts lineItems to the stored JSON format and the create_purchase_order tool still enforces RBAC.",
                    List.of(new ToolCallRecord("create_purchase_order", Map.of())),
                    List.of()
            );
        }
        return new ChatResponse(
                "I can help compare quotations, inspect vendor performance, check inventory, draft purchase orders, and analyze contract risk.",
                List.of(),
                List.of()
        );
    }

    private boolean mentionsContracts(String message) {
        return message.contains("contract")
                || message.contains("policy")
                || message.contains("clause")
                || message.contains("risk")
                || message.contains("liability")
                || message.contains("termination");
    }

    private String ragSystemPrompt() {
        return """
                You are Vendra's procurement intelligence analyst.
                Answer only from the retrieved procurement context.
                If the retrieved context is insufficient, say what is missing.
                Keep the answer concise, practical, and grounded in the cited clauses.
                """;
    }

    private String ragUserPrompt(String question, List<RetrievedChunk> chunks) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("User question:\n").append(question).append("\n\nRetrieved context:\n");
        for (int index = 0; index < chunks.size(); index++) {
            RetrievedChunk chunk = chunks.get(index);
            prompt.append("[").append(index + 1).append("] ")
                    .append(chunk.sourceType()).append(" document ")
                    .append(chunk.sourceDocId())
                    .append(", confidence ")
                    .append(chunk.confidence())
                    .append(":\n")
                    .append(chunk.content())
                    .append("\n\n");
        }
        return prompt.toString();
    }

    private String excerpt(String content) {
        if (content.length() <= 220) {
            return content;
        }
        return content.substring(0, 220) + "...";
    }
}
