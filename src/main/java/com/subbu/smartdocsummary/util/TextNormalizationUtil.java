package com.subbu.smartdocsummary.util;

import com.subbu.smartdocsummary.dto.PresidioEntity;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class TextNormalizationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b\\+?\\d{1,4}?[-.\\s]?\\(?\\d{1,3}\\)?[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}\\b");
    private static final Pattern URL_PATTERN = Pattern.compile("\\bhttps?://[^\\s]+\\b");
    private static final Pattern BROKEN_NAME_PATTERN = Pattern.compile("\\b([A-Za-z]{2,})\\s*\\.\\s*([A-Za-z])\\b");

    public String normalize(String text) {
        if (text == null) {
            return null;
        }

        String normalized = text.trim().replaceAll("\\s+", " ");
        normalized = normalized.replaceAll("\\s*@\\s*", "@");
        return normalizeBrokenPersonNames(normalized);
    }

    private String normalizeBrokenPersonNames(String text) {
        Matcher matcher = BROKEN_NAME_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String firstName = capitalize(matcher.group(1));
            String initial = matcher.group(2).toUpperCase();
            matcher.appendReplacement(result, firstName + " " + initial);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public static List<PresidioEntity> detectEmails(String text) {
        List<PresidioEntity> entities = new ArrayList<>();
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        while (matcher.find()) {
            entities.add(PresidioEntity.builder()
                    .entityType("EMAIL_ADDRESS")
                    .start(matcher.start())
                    .end(matcher.end())
                    .score(1.0)
                    .build());
        }
        return entities;
    }

    public static List<PresidioEntity> detectPhones(String text) {
        List<PresidioEntity> entities = new ArrayList<>();
        Matcher matcher = PHONE_PATTERN.matcher(text);
        while (matcher.find()) {
            entities.add(PresidioEntity.builder()
                    .entityType("PHONE_NUMBER")
                    .start(matcher.start())
                    .end(matcher.end())
                    .score(1.0)
                    .build());
        }
        return entities;
    }

    public static List<PresidioEntity> detectUrls(String text) {
        List<PresidioEntity> entities = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            entities.add(PresidioEntity.builder()
                    .entityType("URL")
                    .start(matcher.start())
                    .end(matcher.end())
                    .score(1.0)
                    .build());
        }
        return entities;
    }
}
