package com.lumina.docs.loadtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone entry point for high-concurrency smoke and load tests.
 * It intentionally avoids scanning the real RAG infrastructure beans.
 */
@SpringBootApplication(scanBasePackages = {
        "com.lumina.docs.loadtest",
        "com.lumina.rag.core.concurrent",
        "com.lumina.rag.core.config"
})
public class LuminaLoadTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(LuminaLoadTestApplication.class, args);
    }
}
