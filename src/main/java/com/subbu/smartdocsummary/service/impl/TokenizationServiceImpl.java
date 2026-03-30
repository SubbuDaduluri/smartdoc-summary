package com.subbu.smartdocsummary.service.impl;

import com.subbu.smartdocsummary.dto.PresidioEntity;
import com.subbu.smartdocsummary.service.TokenizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TokenizationServiceImpl implements TokenizationService {

    // Note: If this service is a Singleton, counters will persist across all requests.
    // Consider moving this to a request-scoped bean or clearing it per document.
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Override
    public String tokenizeAndReplace(String text, List<PresidioEntity> entities, Map<String, String> mapping) {
        if (text == null || text.isBlank() || entities == null || entities.isEmpty()) {
            return text;
        }

        // 1. Filter out overlapping entities to prevent StringBuilder index corruption
        List<PresidioEntity> nonOverlappingEntities = filterOverlaps(entities);

        StringBuilder sb = new StringBuilder(text);

        // 2. Primary sort by End DESC, secondary by Start DESC to handle edge cases
        nonOverlappingEntities.stream()
                .sorted(Comparator.comparingInt(PresidioEntity::getEnd)
                        .thenComparingInt(PresidioEntity::getStart).reversed())
                .forEach(entity -> {
                    try {
                        int start = entity.getStart();
                        int end = entity.getEnd();

                        // Bounds check against current SB length (safety)
                        if (start >= 0 && end <= sb.length() && start < end) {
                            String original = sb.substring(start, end);

                            String token = mapping.computeIfAbsent(original, key -> {
                                String type = entity.getEntityType().toUpperCase();
                                int count = counters.computeIfAbsent(type, k -> new AtomicInteger(0))
                                        .incrementAndGet();
                                return "[" + type + "_" + count + "]";
                            });

                            sb.replace(start, end, token);
                        }
                    } catch (Exception e) {
                        log.error("Failed to replace entity: {} at indices {}-{}",
                                entity.getEntityType(), entity.getStart(), entity.getEnd(), e);
                    }
                });

        return sb.toString();
    }

    private List<PresidioEntity> filterOverlaps(List<PresidioEntity> entities) {
        // Sort by start index ascending
        List<PresidioEntity> sorted = entities.stream()
                .sorted(Comparator.comparingInt(PresidioEntity::getStart))
                .collect(Collectors.toList());

        List<PresidioEntity> result = new java.util.ArrayList<>();
        int lastEnd = -1;

        for (PresidioEntity current : sorted) {
            // If this entity starts after the last one ended, it's safe
            if (current.getStart() >= lastEnd) {
                result.add(current);
                lastEnd = current.getEnd();
            } else {
                log.warn("Skipping overlapping entity: {} ({}-{})",
                        current.getEntityType(), current.getStart(), current.getEnd());
            }
        }
        return result;
    }
}