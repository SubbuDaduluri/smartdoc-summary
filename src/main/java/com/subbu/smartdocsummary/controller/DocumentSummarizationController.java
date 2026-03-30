package com.subbu.smartdocsummary.controller;

import com.subbu.smartdocsummary.dto.ProcessRequest;
import com.subbu.smartdocsummary.dto.ProcessResponse;
import com.subbu.smartdocsummary.service.DocumentSummarizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DocumentSummarizationController {

    private final DocumentSummarizationService documentSummarizationService;

    @PostMapping("/process")
    public ProcessResponse process(@RequestBody ProcessRequest request) {
        return documentSummarizationService.process(request);
    }

    @PostMapping(value = "/process/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProcessResponse processFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (!isAllowedContentType(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }

        log.info("Processing file: {}, size: {}", file.getOriginalFilename(), file.getSize());

        try {
            log.info("Starting text extraction for file: {}", file.getOriginalFilename());
            String text = extractText(file);
            log.info("Text extraction completed, extracted {} characters", text.length());
            if (text.trim().isEmpty()) {
                throw new IllegalArgumentException("No text extracted from file");
            }
            log.info("Starting document summarization");
            ProcessRequest request = new ProcessRequest(text);
            ProcessResponse response = documentSummarizationService.process(request);
            log.info("Document summarization completed");
            return response;
        } catch (IOException e) {
            log.error("Error processing file", e);
            throw new IllegalArgumentException("Failed to process file", e);
        }
    }

    private boolean isAllowedContentType(String contentType) {
        return "application/pdf".equals(contentType) ||
               "application/msword".equals(contentType) ||
               "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType);
    }

    private String extractText(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if ("application/pdf".equals(contentType)) {
            try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(file.getInputStream()))) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } else if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
            try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
                XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                return extractor.getText();
            }
        } else if ("application/msword".equals(contentType)) {
            try (HWPFDocument document = new HWPFDocument(file.getInputStream())) {
                WordExtractor extractor = new WordExtractor(document);
                return extractor.getText();
            }
        } else {
            throw new IllegalArgumentException("Unsupported file type");
        }
    }
}
