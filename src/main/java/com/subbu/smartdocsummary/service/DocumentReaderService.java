package com.subbu.smartdocsummary.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for extracting plain text from documents (PDF, DOCX, TXT, and more)
 * using Apache Tika via Spring AI's {@link DocumentReaderService}.
 *
 * <p>Supports both {@link MultipartFile} and {@link File} inputs. Extracted text
 * from all document pages/sections is merged into a single {@link String}.
 * Text exceeding {@value #MAX_CHARACTERS} characters is safely truncated.</p>
 */
@Service
@Slf4j
public class DocumentReaderService {

    /** Maximum number of characters to retain after extraction. */
    private static final int MAX_CHARACTERS = 500_000;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".doc", ".docx");

    /**
     * Extracts text from an uploaded {@link MultipartFile}.
     *
     * @param file the uploaded file; must not be null or empty
     * @return merged extracted text, never null
     * @throws IllegalArgumentException if the file is null, empty, or not a PDF / DOC / DOCX
     */
    public String extractText(MultipartFile file) {
        Assert.notNull(file, "MultipartFile must not be null");
        Assert.isTrue(!file.isEmpty(), "MultipartFile must not be empty");
        validateFile(file);

        log.info("Extracting text from uploaded file: '{}' ({} bytes)",
                file.getOriginalFilename(), file.getSize());

        return readFromResource(file.getResource());
    }

    /**
     * Extracts text from a local {@link File}.
     *
     * @param file the file on disk; must not be null, must exist and be a regular file
     * @return merged extracted text, never null
     * @throws IllegalArgumentException if the file is null, does not exist, or is not a regular file
     */
    public String extractText(File file) {
        Assert.notNull(file, "File must not be null");
        Assert.isTrue(file.exists() && file.isFile(),
                () -> "File does not exist or is not a regular file: " + file.getAbsolutePath());

        log.info("Extracting text from file: '{}' ({} bytes)", file.getName(), file.length());

        return readFromResource(new FileSystemResource(file));
    }

    private String readFromResource(Resource resource) {
        TikaDocumentReader reader = new TikaDocumentReader(resource);

        String text = reader.get().stream()
                .map(Document::getText)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.joining("\n"));

        if (text.isBlank()) {
            log.warn("No text could be extracted from resource: {}", resource.getDescription());
            return "";
        }

        if (text.length() > MAX_CHARACTERS) {
            log.warn("Extracted text ({} chars) exceeds limit of {}; truncating.",
                    text.length(), MAX_CHARACTERS);
            text = text.substring(0, MAX_CHARACTERS);
        }

        log.info("Text extraction complete: {} characters", text.length());
        return text;
    }

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase()
                : "";

        boolean validContentType = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType);
        boolean validExtension = ALLOWED_EXTENSIONS.stream().anyMatch(filename::endsWith);

        if (!validContentType && !validExtension) {
            throw new IllegalArgumentException(
                    "Unsupported file type: '" + file.getOriginalFilename() +
                    "'. Only PDF, DOC, and DOCX files are allowed.");
        }
    }
}
