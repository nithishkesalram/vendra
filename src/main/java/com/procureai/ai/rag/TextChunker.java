package com.procureai.ai.rag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TextChunker {

    private static final int DEFAULT_CHUNK_WORDS = 140;
    private static final int DEFAULT_OVERLAP_WORDS = 25;

    public List<String> chunk(String text) {
        return chunk(text, DEFAULT_CHUNK_WORDS, DEFAULT_OVERLAP_WORDS);
    }

    public List<String> chunk(String text, int chunkWords, int overlapWords) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> words = Arrays.stream(text.replaceAll("\\s+", " ").trim().split(" "))
                .filter(word -> !word.isBlank())
                .toList();
        if (words.isEmpty()) {
            return List.of();
        }
        if (chunkWords <= overlapWords) {
            throw new IllegalArgumentException("chunkWords must be larger than overlapWords");
        }
        List<String> chunks = new ArrayList<>();
        int index = 0;
        while (index < words.size()) {
            int end = Math.min(index + chunkWords, words.size());
            chunks.add(String.join(" ", words.subList(index, end)));
            if (end == words.size()) {
                break;
            }
            index = end - overlapWords;
        }
        return chunks;
    }
}
