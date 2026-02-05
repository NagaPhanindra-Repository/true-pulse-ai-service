package com.codmer.turepulseai.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentChunker {

    private static final int DEFAULT_MAX_CHARS = 1200;
    private static final int DEFAULT_OVERLAP = 200;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Chunk {
        private int index;
        private String content;
        private String prevContent;
        private String nextContent;
    }

    public List<Chunk> chunk(String text) {
        return chunk(text, DEFAULT_MAX_CHARS, DEFAULT_OVERLAP);
    }

    public List<Chunk> chunk(String text, int maxChars, int overlap) {
        String normalized = text == null ? "" : text.replace("\r", "").trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> rawChunks = splitByParagraphs(normalized, maxChars, overlap);
        List<Chunk> result = new ArrayList<>();

        for (int i = 0; i < rawChunks.size(); i++) {
            String prev = i > 0 ? rawChunks.get(i - 1) : null;
            String next = i < rawChunks.size() - 1 ? rawChunks.get(i + 1) : null;
            result.add(new Chunk(i, rawChunks.get(i), prev, next));
        }

        return result;
    }

    private List<String> splitByParagraphs(String text, int maxChars, int overlap) {
        String[] paragraphs = text.split("\\n\\s*\\n");
        List<String> chunks = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (current.length() + trimmed.length() + 2 > maxChars) {
                addWithOverlap(chunks, current.toString(), overlap);
                current.setLength(0);
            }

            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(trimmed);
        }

        if (current.length() > 0) {
            addWithOverlap(chunks, current.toString(), overlap);
        }

        return chunks;
    }

    private void addWithOverlap(List<String> chunks, String content, int overlap) {
        if (content.length() <= overlap || chunks.isEmpty()) {
            chunks.add(content);
            return;
        }

        String prev = chunks.get(chunks.size() - 1);
        String overlapText = prev.substring(Math.max(0, prev.length() - overlap));
        if (!content.startsWith(overlapText)) {
            content = overlapText + "\n" + content;
        }
        chunks.add(content);
    }
}

