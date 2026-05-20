package com.lumina.docs.loadtest;

import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LoadTestScenarioService {

    private final Map<String, String> documents = new ConcurrentHashMap<>();
    private final AtomicLong realGenerationCount = new AtomicLong();

    public SeedResult seed(int documentsCount, int paragraphsPerDocument) {
        documents.clear();
        int safeDocs = Math.max(1, Math.min(documentsCount, 100_000));
        int safeParagraphs = Math.max(1, Math.min(paragraphsPerDocument, 50));

        for (int i = 1; i <= safeDocs; i++) {
            String docId = String.format(Locale.ROOT, "lt-doc-%05d", i);
            StringBuilder text = new StringBuilder();
            text.append("Document ").append(docId).append(" covers Lumina RAG concurrency, cache, retrieval, and SSE behavior.\n");
            for (int p = 1; p <= safeParagraphs; p++) {
                text.append("Paragraph ").append(p)
                        .append(": tenant=").append(i % 16)
                        .append(", sku=").append(10_000 + i)
                        .append(", policy=load-test, topic=high-concurrency simulation.\n");
            }
            documents.put(docId, text.toString());
        }
        return new SeedResult(safeDocs, safeParagraphs, Instant.now().toString());
    }

    public List<String> buildAnswerTokens(String query, int maxTokens, long thinkMillis) {
        realGenerationCount.incrementAndGet();
        sleep(thinkMillis);

        String pickedDoc = pickDocument(query);
        String answer = "Lumina load-test answer. query=\"" + nullToEmpty(query) + "\"; "
                + "matchedDoc=\"" + pickedDoc + "\"; "
                + "evidence=\"" + summarize(documents.get(pickedDoc)) + "\"; "
                + "realGenerationCount=" + realGenerationCount.get() + ".";

        int safeMaxTokens = Math.max(1, Math.min(maxTokens, 200));
        String[] words = answer.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < words.length && i < safeMaxTokens; i++) {
            tokens.add(words[i] + (i == words.length - 1 ? "" : " "));
        }
        return tokens;
    }

    public Map<String, Object> stats() {
        return Map.of(
                "documents", documents.size(),
                "realGenerationCount", realGenerationCount.get()
        );
    }

    private String pickDocument(String query) {
        if (documents.isEmpty()) {
            seed(100, 4);
        }
        int size = documents.size();
        int offset = Math.abs(nullToEmpty(query).hashCode()) % size;
        return documents.keySet().stream().skip(offset).findFirst()
                .orElseGet(() -> documents.keySet().iterator().next());
    }

    private String summarize(String value) {
        String text = nullToEmpty(value).replace('\n', ' ');
        return text.length() <= 220 ? text : text.substring(0, 220);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Data
    public static class ChatRequest {
        private String query = "How does Lumina handle high concurrency?";
        private String sessionId = "loadtest-session";
        private String dedupeKey;
        private int maxTokens = 48;
        private long thinkMillis = 150;
        private long tokenMillis = 20;

        public String effectiveDedupeKey() {
            if (dedupeKey != null && !dedupeKey.trim().isEmpty()) {
                return dedupeKey.trim();
            }
            return sessionId + ":" + query;
        }
    }

    public static class SeedResult {
        public final int documents;
        public final int paragraphsPerDocument;
        public final String seededAt;

        public SeedResult(int documents, int paragraphsPerDocument, String seededAt) {
            this.documents = documents;
            this.paragraphsPerDocument = paragraphsPerDocument;
            this.seededAt = seededAt;
        }
    }
}
