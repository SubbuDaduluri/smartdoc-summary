package com.subbu.smartdocsummary.service;

import com.subbu.smartdocsummary.service.impl.LlmServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LlmServiceImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private LlmServiceImpl llmService;

    @BeforeEach
    void setUp() {
        llmService = new LlmServiceImpl(chatClient);
    }

    @Test
    void shouldReturnAsIsForBlankInput() {
        String result = llmService.summarize("   ");

        assertThat(result).isEqualTo("   ");
        verifyNoInteractions(chatClient);
    }

    @Test
    void shouldFallbackToOriginalTextWhenLlmReturnsBlank() {
        String input = "Tokenized text content";
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content()).thenReturn("   ");

        String result = llmService.summarize(input);

        assertThat(result).isEqualTo(input);
    }

    @Test
    void shouldCleanArtifactsAndReturnOnlySummary() {
        String input = "Customer_A paid invoice";
        String llmOutput = "Summary: Here is a summary: Customer_A completed payment.\nNote: Additional details omitted.";
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content()).thenReturn(llmOutput);

        String result = llmService.summarize(input);

        assertThat(result).isEqualTo("Customer_A completed payment.");
    }

    @Test
    void shouldFallbackToOriginalTextOnException() {
        String input = "Customer_A update";
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new RuntimeException("downstream error"));

        String result = llmService.summarize(input);

        assertThat(result).isEqualTo(input);
    }

}

