package com.procureai.ai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextChunkerTest {

    @Test
    void chunksTextWithOverlap() {
        TextChunker chunker = new TextChunker();
        String text = "one two three four five six seven eight nine ten";

        assertThat(chunker.chunk(text, 4, 1))
                .containsExactly(
                        "one two three four",
                        "four five six seven",
                        "seven eight nine ten"
                );
    }

    @Test
    void returnsEmptyListForBlankText() {
        assertThat(new TextChunker().chunk("  ")).isEmpty();
    }
}
