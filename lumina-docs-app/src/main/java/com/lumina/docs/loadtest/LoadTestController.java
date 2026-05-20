package com.lumina.docs.loadtest;

import com.lumina.rag.core.concurrent.RequestDeduplicator;
import com.lumina.rag.core.config.LuminaAsyncConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loadtest")
@RequiredArgsConstructor
public class LoadTestController {

    private final LoadTestScenarioService scenarioService;
    private final RequestDeduplicator deduplicator;

    @Qualifier(LuminaAsyncConfig.RAG_EXECUTOR_NAME)
    private final Executor ragExecutor;

    @PostMapping("/seed")
    public LoadTestScenarioService.SeedResult seed(
            @RequestParam(defaultValue = "1000") int documents,
            @RequestParam(defaultValue = "4") int paragraphs) {
        return scenarioService.seed(documents, paragraphs);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return scenarioService.stats();
    }

    @PostMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chatStream(@RequestBody LoadTestScenarioService.ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        CompletableFuture.runAsync(() -> streamWithSingleflight(request, emitter), ragExecutor);
        return emitter;
    }

    private void streamWithSingleflight(LoadTestScenarioService.ChatRequest request, SseEmitter emitter) {
        try {
            String answer = deduplicator.execute("loadtest:" + request.effectiveDedupeKey(), () -> {
                List<String> tokens = scenarioService.buildAnswerTokens(
                        request.getQuery(), request.getMaxTokens(), request.getThinkMillis());
                return tokens.stream().collect(Collectors.joining());
            });

            sendTokenized(emitter, answer, request.getTokenMillis());
            emitter.send(SseEmitter.event().name("DONE").data("[DONE]"));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    private void sendTokenized(SseEmitter emitter, String answer, long tokenMillis) throws IOException {
        String[] tokens = answer.split("(?<=\\s)");
        for (String token : tokens) {
            emitter.send(SseEmitter.event().data(token));
            LoadTestScenarioService.sleep(tokenMillis);
        }
    }
}
