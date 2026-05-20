# Lumina High-Concurrency Simulation

This load-test path exercises the HTTP/SSE layer and Lumina singleflight deduplication without requiring MySQL, Kafka, Redis, Elasticsearch, or a real LLM.

## Start The Simulation App

```powershell
mvn -pl lumina-docs-app -am spring-boot:run "-Dspring-boot.run.main-class=com.lumina.docs.loadtest.LuminaLoadTestApplication" "-Dspring-boot.run.profiles=loadtest"
```

## Seed Data

```powershell
curl -X POST "http://localhost:8080/api/loadtest/seed?documents=5000&paragraphs=6"
```

## Smoke Test SSE

```powershell
curl -N -X POST "http://localhost:8080/api/loadtest/chat/stream" -H "Content-Type: application/json" -d "{\"query\":\"How does Lumina handle high concurrency?\",\"sessionId\":\"s1\",\"dedupeKey\":\"same-question-hot-key\"}"
```

## Run k6

```powershell
k6 run loadtest/k6/lumina-chat-stream.js
```

Use `BASE_URL` to point at another environment:

```powershell
$env:BASE_URL="http://your-host:8080"; k6 run loadtest/k6/lumina-chat-stream.js
```
