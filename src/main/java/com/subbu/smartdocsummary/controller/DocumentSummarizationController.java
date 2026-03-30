package com.subbu.smartdocsummary.controller;

import com.subbu.smartdocsummary.dto.ProcessRequest;
import com.subbu.smartdocsummary.dto.ProcessResponse;
import com.subbu.smartdocsummary.service.DocumentSummarizationService;
import com.subbu.smartdocsummary.service.DocumentReaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DocumentSummarizationController {

    private final DocumentSummarizationService documentSummarizationService;
    private final DocumentReaderService documentReaderService;

    @PostMapping("/process")
    public ProcessResponse process(@RequestBody ProcessRequest request) {
        return documentSummarizationService.process(request);
    }

    @PostMapping(value = "/process/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProcessResponse processFile(@RequestParam("file") MultipartFile file) {
        log.info("Processing file: '{}', size: {} bytes",
                file.getOriginalFilename(), file.getSize());

        String text = documentReaderService.extractText(file);

        if (text.isBlank()) {
            throw new IllegalArgumentException("No text could be extracted from the uploaded file");
        }

        log.info("Text extraction completed: {} characters; starting summarization", text.length());
        ProcessResponse response = documentSummarizationService.process(new ProcessRequest(text));
        log.info("Document summarization completed");
        return response;
    }
}
