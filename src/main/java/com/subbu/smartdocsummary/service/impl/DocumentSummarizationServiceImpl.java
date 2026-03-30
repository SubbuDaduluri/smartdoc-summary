package com.subbu.smartdocsummary.service.impl;

import com.subbu.smartdocsummary.client.PresidioClient;
import com.subbu.smartdocsummary.dto.PresidioEntity;
import com.subbu.smartdocsummary.dto.ProcessRequest;
import com.subbu.smartdocsummary.dto.ProcessResponse;
import com.subbu.smartdocsummary.service.DocumentSummarizationService;
import com.subbu.smartdocsummary.service.LlmService;
import com.subbu.smartdocsummary.service.TokenizationService;
import com.subbu.smartdocsummary.util.TextNormalizationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentSummarizationServiceImpl implements DocumentSummarizationService {

    private static final Pattern WORD_PATTERN = Pattern.compile("\\b[A-Za-z]+\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?://|www\\.)\\S+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_WITH_INITIAL_PATTERN = Pattern.compile("\\b([A-Z][a-z]{3,})\\s+([A-Z])\\b");
    private static final Pattern CONTEXT_PATTERN = Pattern.compile("\\b(name|customer|user)\\b", Pattern.CASE_INSENSITIVE);

    private static final Set<String> PERSON_KEYWORD_DENYLIST = Set.of(
            "login", "admin", "file", "username", "userid", "password", "account", "system", "server", "api",
            "document", "name", "customer", "user"
    );

    private static final double CUSTOM_PERSON_BASE_SCORE = 0.60;
    private static final double CUSTOM_PERSON_CONTEXT_SCORE = 0.75;

    private final PresidioClient presidioClient;
    private final TokenizationService tokenizationService;
    private final LlmService llmService;

    @Override
    public ProcessResponse process(ProcessRequest request) {
        log.info("Starting text processing");

        String normalizedText = TextNormalizationUtil.normalize(request.text());
        log.debug("Text normalized");

        // Detect PII entities using Presidio ML-based analyzer
        List<PresidioEntity> entities = detectAllEntities(normalizedText);
        log.info("Detected {} PII entities", entities.size());

        // Tokenize and replace entities deterministically
        Map<String, String> mapping = new HashMap<>();
        String tokenizedText = tokenizationService.tokenizeAndReplace(normalizedText, entities, mapping);
        log.debug("Text tokenized with {} token mappings", mapping.size());

        // Summarize using LLM
        String processedText = llmService.summarize(tokenizedText);
        log.debug("Text summarized");

        // Replace tokens with actual values in the final response
        String finalResponse = reverseTokensInResponse(processedText, mapping);
        log.debug("Token replacement completed with {} mappings", mapping.size());

        return new ProcessResponse(finalResponse);
    }

    /**
     * Detects PII entities using Presidio ML-based analysis.
     * Validates all entities to ensure they have valid indices within text bounds.
     */
    private List<PresidioEntity> detectAllEntities(String text) {
        List<PresidioEntity> presidioEntities = detectPresidioEntities(text);
        int presidioPersonCount = countEntityType(presidioEntities, "PERSON");
        log.debug("Presidio detection found {} entities ({} PERSON)", presidioEntities.size(), presidioPersonCount);

        List<PresidioEntity> customPersonEntities = detectCustomPersonEntities(text, presidioEntities);
        log.debug("Custom PERSON detection found {} entities", customPersonEntities.size());

        List<PresidioEntity> merged = mergeEntities(presidioEntities, customPersonEntities);
        log.debug("Merged entity count: {}", merged.size());

        // Validate entities (bounds checking)
        List<PresidioEntity> validated = validateEntities(merged, text);
        log.debug("After validation: {} valid entities", validated.size());

        return validated;
    }


    /**
     * Detects PII using Presidio's ML-based analyzer.
     */
    private List<PresidioEntity> detectPresidioEntities(String text) {
        try {
            List<PresidioEntity> presidioEntities = presidioClient.analyze(text).block();
            return presidioEntities == null ? List.of() : presidioEntities;
        } catch (Exception e) {
            log.warn("Presidio analysis failed, continuing with custom PERSON detection", e);
            return List.of();
        }
    }

    private List<PresidioEntity> detectCustomPersonEntities(String text, List<PresidioEntity> existingEntities) {
        List<PresidioEntity> customEntities = new ArrayList<>();
        Set<String> occupiedRanges = new HashSet<>();
        for (PresidioEntity entity : existingEntities) {
            occupiedRanges.add(entity.getStart() + ":" + entity.getEnd());
        }

        Matcher wordMatcher = WORD_PATTERN.matcher(text);
        while (wordMatcher.find()) {
            int start = wordMatcher.start();
            int end = wordMatcher.end();
            String candidate = text.substring(start, end);

            if (!isEligibleSingleWordPerson(candidate)) {
                continue;
            }
            if (isFalsePositiveCandidate(candidate)) {
                continue;
            }
            if (isFollowedBySingleLetterInitial(text, end)) {
                continue;
            }

            String key = start + ":" + end;
            if (occupiedRanges.contains(key)) {
                continue;
            }

            double score = hasContextCueAround(text, start, end) ? CUSTOM_PERSON_CONTEXT_SCORE : CUSTOM_PERSON_BASE_SCORE;
            customEntities.add(PresidioEntity.builder()
                    .entityType("PERSON")
                    .start(start)
                    .end(end)
                    .score(score)
                    .build());
            occupiedRanges.add(key);
        }

        Matcher nameWithInitialMatcher = NAME_WITH_INITIAL_PATTERN.matcher(text);
        while (nameWithInitialMatcher.find()) {
            int start = nameWithInitialMatcher.start();
            int end = nameWithInitialMatcher.end();
            String key = start + ":" + end;
            if (!hasContextCueAround(text, start, end) || occupiedRanges.contains(key)) {
                continue;
            }
            customEntities.add(PresidioEntity.builder()
                    .entityType("PERSON")
                    .start(start)
                    .end(end)
                    .score(CUSTOM_PERSON_CONTEXT_SCORE)
                    .build());
            occupiedRanges.add(key);
        }

        return customEntities;
    }

    private List<PresidioEntity> mergeEntities(List<PresidioEntity> presidioEntities, List<PresidioEntity> customEntities) {
        List<PresidioEntity> merged = new ArrayList<>();
        for (PresidioEntity entity : presidioEntities) {
            addWithOverlapResolution(merged, entity);
        }
        for (PresidioEntity entity : customEntities) {
            addWithOverlapResolution(merged, entity);
        }
        merged.sort(Comparator.comparingInt(PresidioEntity::getStart).thenComparingInt(PresidioEntity::getEnd));
        return merged;
    }

    private void addWithOverlapResolution(List<PresidioEntity> merged, PresidioEntity candidate) {
        for (int i = 0; i < merged.size(); i++) {
            PresidioEntity existing = merged.get(i);
            if (sameSpan(existing, candidate) || overlaps(existing, candidate)) {
                if (candidate.getScore() > existing.getScore()) {
                    merged.set(i, candidate);
                }
                return;
            }
        }
        merged.add(candidate);
    }

    private boolean sameSpan(PresidioEntity first, PresidioEntity second) {
        return first.getStart() == second.getStart() && first.getEnd() == second.getEnd();
    }

    private boolean overlaps(PresidioEntity first, PresidioEntity second) {
        return first.getStart() < second.getEnd() && second.getStart() < first.getEnd();
    }

    private boolean isEligibleSingleWordPerson(String candidate) {
        return candidate.length() > 3
                && isAlphabetic(candidate)
                && Character.isUpperCase(candidate.charAt(0))
                && candidate.substring(1).chars().allMatch(Character::isLowerCase);
    }

    private boolean isAlphabetic(String candidate) {
        return candidate.chars().allMatch(Character::isLetter);
    }

    private boolean isFalsePositiveCandidate(String candidate) {
        String lower = candidate.toLowerCase();
        if (PERSON_KEYWORD_DENYLIST.contains(lower)) {
            return true;
        }
        return EMAIL_PATTERN.matcher(candidate).matches() || URL_PATTERN.matcher(candidate).matches();
    }

    private boolean isFollowedBySingleLetterInitial(String text, int candidateEnd) {
        int index = candidateEnd;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index < text.length() - 1
                && Character.isUpperCase(text.charAt(index))
                && (index + 1 == text.length() || !Character.isLetter(text.charAt(index + 1)));
    }

    private boolean hasContextCueAround(String text, int start, int end) {
        int windowStart = Math.max(0, start - 35);
        int windowEnd = Math.min(text.length(), end + 35);
        String window = text.substring(windowStart, windowEnd);
        return CONTEXT_PATTERN.matcher(window).find();
    }

    private int countEntityType(List<PresidioEntity> entities, String entityType) {
        return (int) entities.stream().filter(entity -> entityType.equals(entity.getEntityType())).count();
    }

    /**
     * Validates entities to ensure they have valid indices within text bounds.
     */
    private List<PresidioEntity> validateEntities(List<PresidioEntity> entities, String text) {
        return entities.stream()
                .filter(entity -> isValidEntity(entity, text))
                .toList();
    }

    /**
     * Checks if entity has valid indices and doesn't exceed text bounds.
     */
    private boolean isValidEntity(PresidioEntity entity, String text) {
        return entity.getStart() >= 0 
                && entity.getEnd() <= text.length() 
                && entity.getStart() < entity.getEnd();
    }

    /**
     * Replaces all tokens in the response with their original values.
     * This ensures the final response contains readable actual values instead of placeholders.
     */
    private String reverseTokensInResponse(String response, Map<String, String> mapping) {
        String result = response;
        // Replace tokens with actual values
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String originalValue = entry.getKey();
            String token = entry.getValue();
            result = result.replace(token, originalValue);
        }
        return result;
    }
}
