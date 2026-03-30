package com.subbu.smartdocsummary.controller;

import com.subbu.smartdocsummary.dto.ProcessRequest;
import com.subbu.smartdocsummary.dto.ProcessResponse;
import com.subbu.smartdocsummary.service.DocumentSummarizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentSummarizationController.class)
class TextProcessingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentSummarizationService documentSummarizationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testProcess() throws Exception {
        ProcessRequest request = new ProcessRequest("John Doe john.doe@example.com");
        // Response should contain actual values, not tokens
        ProcessResponse response = new ProcessResponse("Here is a summary:\n\nA person John Doe has an email john.doe@example.com");

        when(documentSummarizationService.process(any(ProcessRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedText").value("Here is a summary:\n\nA person John Doe has an email john.doe@example.com"));
    }
}
