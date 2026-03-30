package com.subbu.smartdocsummary.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.subbu.smartdocsummary.dto.PresidioAnalyzeRequest;
import com.subbu.smartdocsummary.dto.PresidioEntity;
import com.subbu.smartdocsummary.exception.PresidioClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class PresidioClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${presidio.url}")
    private String presidioUrl;

    /**
     * Entities to detect (broad list)
     */
    private static final List<String> ENTITIES = List.of(
            "PERSON",
            "EMAIL_ADDRESS",
            "PHONE_NUMBER",
            "LOCATION",
            "URL",
            "IP_ADDRESS",
            "CREDIT_CARD",
            "CRYPTO",
            "IBAN_CODE",
            "US_SSN",
            "US_DRIVER_LICENSE",
            "US_PASSPORT",
            "US_BANK_NUMBER",
            "UK_NHS",
            "UK_NINO",
            "DATE_TIME" // detected but filtered later
    );

    /**
     * Allowed PII (only these go downstream)
     * DATE_TIME intentionally excluded
     */
    private static final Set<String> ALLOWED_PII = Set.of(
            "PERSON",
            "EMAIL_ADDRESS",
            "PHONE_NUMBER",
            "LOCATION",
            "CREDIT_CARD",
            "IP_ADDRESS",
            "URL",
            "CRYPTO",
            "IBAN_CODE",
            "US_SSN",
            "US_DRIVER_LICENSE",
            "US_PASSPORT",
            "US_BANK_NUMBER",
            "UK_NHS",
            "UK_NINO"
    );

    private static final double SCORE_THRESHOLD = 0.5;

    public Mono<List<PresidioEntity>> analyze(String text) {

        if (text == null || text.isBlank()) {
            return Mono.just(List.of());
        }

        PresidioAnalyzeRequest request = PresidioAnalyzeRequest.builder()
                .text(text)
                .entities(ENTITIES)
                .language("en")
                .scoreThreshold(SCORE_THRESHOLD)
                .build();

        return webClient.post()
                .uri(presidioUrl + "/analyze")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(PresidioEntity.class)
                .collectList()

                // Core filtering pipeline
                .map(this::filterEntities)

                // Retry for transient failures
                .retryWhen(Retry.backoff(2, Duration.ofMillis(300))
                        .filter(this::isRetryableError))

                // Timeout protection
                .timeout(Duration.ofSeconds(5))

                .doOnNext(entities ->
                        log.debug("Presidio returned {} filtered entities", entities.size())
                )

                .onErrorResume(error -> {
                    log.error("Presidio call failed. Falling back to empty entity list.", error);
                    return Mono.just(List.of()); // graceful degradation
                });
    }

    /**
     * Centralized filtering logic (clean & testable)
     */
    private List<PresidioEntity> filterEntities(List<PresidioEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.stream()
                .filter(Objects::nonNull)

                // Score validation (score is primitive double)
                .filter(e -> e.getScore() >= SCORE_THRESHOLD)

                // Only allowed PII (DATE_TIME excluded automatically)
                .filter(e -> ALLOWED_PII.contains(e.getEntityType()))

                // Valid index safety (avoid downstream crashes)
                .filter(e -> e.getStart() >= 0 && e.getEnd() > e.getStart())

                .toList();
    }

    /**
     * Retry only for transient issues (network, 5xx)
     */
    private boolean isRetryableError(Throwable throwable) {
        return !(throwable instanceof PresidioClientException);
    }
}