# Smart Document Summary

Smart Document Summary is a Spring Boot application for secure document summarization.

Upload a PDF, DOC, or DOCX file to get a summarized version.

---

## Features

- Upload and summarize PDF, DOC, and DOCX files
- Extract text from uploaded documents and return concise output
- Process raw text directly through API
- Optional local or cloud LLM provider configuration

---

## Tech Stack

- Java 21
- Spring Boot 3.5.x
- Spring AI
- Apache PDFBox and Apache POI
- Maven

---

## Run Locally

```bash
mvn clean install
mvn spring-boot:run
```

Application URL:

```text
http://localhost:8080
```

---

## API Endpoints

### Upload File and Summarize

`POST /api/process/file`

Accepted file types:

- `application/pdf`
- `application/msword`
- `application/vnd.openxmlformats-officedocument.wordprocessingml.document`

### Summarize Raw Text

`POST /api/process`

Request:

```json
{
  "text": "Your document content here"
}
```

Response:

```json
{
  "processedText": "Summarized output"
}
```

---

## Author

Subramanyam D
