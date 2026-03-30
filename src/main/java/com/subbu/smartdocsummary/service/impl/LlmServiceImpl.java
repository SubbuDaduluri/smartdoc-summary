package com.subbu.smartdocsummary.service.impl;

import com.subbu.smartdocsummary.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmServiceImpl implements LlmService {

    /**
     * Simple but strong system prompt
     */
    private static final String SYSTEM_PROMPT = """
        You are a professional enterprise document summarizer.

        STRICT RULES:
        - Return ONLY the summary.
        - No introductions or notes.
        - Preserve tokens exactly (Customer_A, Email_1).
        - NEVER create tokens like Person_1.
        - NEVER change numbers or facts.
        - Fix grammar issues.
        - Avoid repetition and hallucination.
        """;

    private static final String USER_PROMPT = """
        Summarize the content into a clean professional paragraph (150–200 words).

        REQUIREMENTS:
        - Use ONLY given content
        - Preserve tokens exactly
        - Fix grammar
        - Do NOT hallucinate

        TEXT:
        %s
        """;

    private final ChatClient chatClient;

    @Override
    public String summarize(String text) {

        if (text == null || text.isBlank()) {
            return text;
        }

        try {
            log.debug("Calling LLM for summarization");

            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(USER_PROMPT.formatted(text))
                    .call()
                    .content();

            if (response == null || response.isBlank()) {
                return text;
            }

            String cleaned = response.trim();

            // Minimal safety (important)
            if (cleaned.matches(".*Person_\\d+.*")) {
                log.warn("Invalid token detected, fallback to original text");
                return text;
            }

            return cleaned;

        } catch (Exception e) {
            log.warn("LLM summarization failed, returning original text", e);
            return text;
        }
    }
}