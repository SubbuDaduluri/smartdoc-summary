# SmartDoc Summary

A Spring Boot application that securely extracts text from uploaded documents and produces AI-generated summaries, with built-in PII detection and redaction via Microsoft Presidio.

---

## Features

- **Multi-format document support** — PDF, DOCX, DOC, TXT, and any format supported by Apache Tika
- **AI-powered summarization** — pluggable LLM backend (Ollama or OpenAI)
- **PII detection & redaction** — integrates with Microsoft Presidio Analyzer before sending text to the LLM
- **Raw text processing** — submit plain text directly without uploading a file
- **Large-file safety** — extracted text is capped at 500,000 characters
- **Configurable multipart limits** — default 10 MB upload limit (configurable)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.x |
| AI / LLM | Spring AI 1.1.3 (Ollama · OpenAI) |
| Document reading | Apache Tika via `spring-ai-tika-document-reader` |
| PII detection | Microsoft Presidio Analyzer REST API |
| Vector store | Qdrant (`spring-ai-starter-vector-store-qdrant`) |
| Build | Maven (Maven Wrapper included) |
| Utilities | Lombok |

---

## Prerequisites

| Requirement | Notes |
|---|---|
| Java 21+ | `java -version` to verify |
| Maven 3.9+ **or** use `./mvnw` | Wrapper included |
| Ollama (local LLM) | `ollama serve` + `ollama pull llama3` — required when `app.llm.provider=ollama` |
| OpenAI API key | Set `OPENAI_API_KEY` env var — required when `app.llm.provider=openai` |
| Presidio Analyzer | Running on `http://localhost:5002` (configurable) |
| Qdrant | Running on default port (optional, for vector-store features) |

---

## Configuration

All settings live in `src/main/resources/application.yaml`:

```yaml
server:
  port: 8080

spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3

    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.3

presidio:
  url: http://localhost:5002

app:
  llm:
    provider: ollama   # switch to "openai" to use OpenAI
```

---

## Run Locally

```bash
# 1. Clone and build
git clone <repo-url>
cd smartdoc-summary
./mvnw clean package -DskipTests

# 2. Start dependencies (example using Docker)
docker run -d -p 5002:5001 mcr.microsoft.com/presidio-analyzer
docker run -d -p 6333:6333 qdrant/qdrant
ollama serve &
ollama pull llama3

# 3. Run the application
./mvnw spring-boot:run
```

Application available at: `http://localhost:8080`

---

## API Endpoints

### Upload a Document and Summarize

```
POST /api/process/file
Content-Type: multipart/form-data
```

| Parameter | Type | Description |
|---|---|---|
| `file` | `MultipartFile` | Document to summarize (PDF, DOCX, DOC, TXT, …) |

**Example (curl):**

```bash
curl -X POST http://localhost:8080/api/process/file \
  -F "file=@/path/to/document.pdf"
```

**Response:**

```json
{
  "processedText": "This document discusses..."
}
```

---

### Summarize Raw Text

```
POST /api/process
Content-Type: application/json
```

**Request body:**

```json
{
  "text": "Paste your document content here."
}
```

**Response:**

```json
{
  "processedText": "Summarized output..."
}
```

---

## Project Structure

```
src/main/java/com/subbu/smartdocsummary/
├── SmartdocSummaryApplication.java
├── client/
│   └── PresidioClient.java              # REST client for Presidio Analyzer
├── config/
│   ├── AppProperties.java               # Typed config (@ConfigurationProperties)
│   ├── QdrantCollectionInitializer.java
│   └── WebClientConfig.java
├── controller/
│   ├── DocumentSummarizationController.java   # /api/process and /api/process/file
│   └── GlobalExceptionHandler.java
├── dto/
│   ├── ProcessRequest.java
│   ├── ProcessResponse.java
│   └── Presidio*.java                   # Presidio request/response DTOs
├── service/
│   ├── DocumentReaderService.java       # Document text extraction (Apache Tika)
│   ├── DocumentSummarizationService.java
│   ├── LlmService.java
│   ├── TokenizationService.java
│   └── impl/
└── util/
```

---

## Document Reading — DocumentReaderService

Text extraction is handled exclusively by `DocumentReaderService` using Spring AI's `TikaDocumentReader` (backed by Apache Tika's `AutoDetectParser`).

- Accepts `MultipartFile` or `java.io.File`
- Auto-detects file format — no explicit MIME-type mapping required
- Merges all pages/sections into a single `String`
- Truncates output at **500,000 characters** for large documents
- Logs extraction size and warns on truncation

---

## Running Tests

```bash
./mvnw test
```

Test reports are written to `target/surefire-reports/`.

---

## Author

Subramanyam D
