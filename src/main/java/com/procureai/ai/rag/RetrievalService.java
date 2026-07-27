package com.procureai.ai.rag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetrievalService {

    private static final Map<String, Set<String>> SYNONYM_MAP = new HashMap<>();

    static {
        addSynonyms("indemnify", "indemnification", "liability", "harmless", "claim", "damage");
        addSynonyms("terminate", "termination", "cancel", "cancellation", "exit", "expiration");
        addSynonyms("penalty", "penalties", "fine", "liquidated", "damages", "breach");
        addSynonyms("renewal", "renew", "auto-renew", "automatic", "extension");
        addSynonyms("payment", "pay", "invoicing", "remittance", "billing", "fee", "due");
        addSynonyms("confidential", "confidentiality", "privacy", "nondisclosure", "gdpr");
        addSynonyms("warranty", "warranties", "guarantee", "defect", "remedy");
        addSynonyms("sla", "uptime", "availability", "performance", "metric");
    }

    private static void addSynonyms(String... terms) {
        Set<String> set = new HashSet<>(Arrays.asList(terms));
        for (String term : terms) {
            SYNONYM_MAP.put(term, set);
        }
    }

    private final DocumentChunkRepository documentChunkRepository;

    public RetrievalService(DocumentChunkRepository documentChunkRepository) {
        this.documentChunkRepository = documentChunkRepository;
    }

    @Transactional(readOnly = true)
    public List<RetrievedChunk> retrieveRelevantChunks(String query, Long vendorId, int topK) {
        return retrieveRelevantChunks(query, vendorId, null, topK);
    }

    @Transactional(readOnly = true)
    public List<RetrievedChunk> retrieveRelevantChunks(String query, Long vendorId, Long contractId, int topK) {
        List<DocumentChunk> chunks;
        if (contractId != null) {
            chunks = documentChunkRepository.findBySourceDocId(contractId);
        } else if (vendorId != null) {
            chunks = documentChunkRepository.findByVendorId(vendorId);
        } else {
            chunks = documentChunkRepository.findAll();
        }

        if (chunks.isEmpty() || query == null || query.isBlank()) {
            return List.of();
        }

        Set<String> queryTokens = tokenize(query);
        Set<String> expandedQueryTerms = expandWithSynonyms(queryTokens);
        String rawQueryLower = query.toLowerCase(Locale.ROOT).trim();

        return chunks.stream()
                .map(chunk -> scoreChunk(chunk, queryTokens, expandedQueryTerms, rawQueryLower))
                .filter(chunk -> chunk.score() > 0)
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(topK)
                .toList();
    }

    private RetrievedChunk scoreChunk(
            DocumentChunk chunk,
            Set<String> queryTokens,
            Set<String> expandedTerms,
            String rawQueryLower
    ) {
        String content = chunk.getContent() != null ? chunk.getContent() : "";
        String contentLower = content.toLowerCase(Locale.ROOT);
        Set<String> contentTokens = tokenize(content);

        if (contentTokens.isEmpty() || expandedTerms.isEmpty()) {
            return new RetrievedChunk(
                    chunk.getId(),
                    chunk.getSourceDocId(),
                    chunk.getVendorId(),
                    chunk.getSourceType(),
                    content,
                    0.0,
                    chunk.getMetadataJson()
            );
        }

        // Direct token matches with term weighting
        double totalWeightedMatches = 0.0;
        double maxPossibleWeight = 0.0;

        for (String queryToken : queryTokens) {
            double weight = getTermWeight(queryToken);
            maxPossibleWeight += weight;
            if (contentTokens.contains(queryToken)) {
                totalWeightedMatches += weight;
            } else {
                // Check synonym matches
                Set<String> synonyms = SYNONYM_MAP.getOrDefault(queryToken, Set.of());
                boolean synonymMatched = synonyms.stream().anyMatch(contentTokens::contains);
                if (synonymMatched) {
                    totalWeightedMatches += weight * 0.75;
                }
            }
        }

        double tokenScore = maxPossibleWeight > 0 ? (totalWeightedMatches / maxPossibleWeight) : 0.0;

        // Exact phrase / n-gram substring boost
        double phraseBoost = 0.0;
        if (rawQueryLower.length() > 3 && contentLower.contains(rawQueryLower)) {
            phraseBoost = 0.35;
        }

        double finalScore = Math.min(1.0, Math.round((tokenScore * 0.75 + phraseBoost) * 100.0) / 100.0);

        return new RetrievedChunk(
                chunk.getId(),
                chunk.getSourceDocId(),
                chunk.getVendorId(),
                chunk.getSourceType(),
                content,
                finalScore,
                chunk.getMetadataJson()
        );
    }

    private double getTermWeight(String token) {
        return switch (token) {
            case "indemnification", "indemnify", "liability", "termination", "penalty", "gdpr", "arbitration" -> 2.5;
            case "renewal", "warranty", "breach", "claim", "confidentiality", "sla", "liquidated" -> 2.0;
            case "contract", "agreement", "party", "supplier", "buyer", "vendor", "clause" -> 1.2;
            default -> 1.0;
        };
    }

    private Set<String> expandWithSynonyms(Set<String> queryTokens) {
        Set<String> expanded = new HashSet<>(queryTokens);
        for (String token : queryTokens) {
            if (SYNONYM_MAP.containsKey(token)) {
                expanded.addAll(SYNONYM_MAP.get(token));
            }
        }
        return expanded;
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").split("\\s+"))
                .filter(token -> token.length() > 1)
                .collect(Collectors.toSet());
    }
}

