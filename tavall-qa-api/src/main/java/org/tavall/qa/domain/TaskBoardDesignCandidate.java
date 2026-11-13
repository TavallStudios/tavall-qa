package org.tavall.qa.domain;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

/** One named live design direction. URLs are HTTPS documents, never executable schemes. */
public record TaskBoardDesignCandidate(String key, String name, String previewUrl, String rationale,
                                       Map<String, List<String>> optionParameters) {
    public TaskBoardDesignCandidate(String key, String name, String previewUrl, String rationale) {
        this(key, name, previewUrl, rationale, Map.of());
    }

    public TaskBoardDesignCandidate {
        key = text(key, "key", 1).toUpperCase(Locale.ROOT);
        if (!key.matches("[A-D]")) throw new IllegalArgumentException("Candidate key must be A, B, C or D");
        name = text(name, "name", 120);
        rationale = text(rationale, "rationale", 2000);
        previewUrl = text(previewUrl, "previewUrl", 2048);
        URI uri = URI.create(previewUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Preview URL must be an HTTPS document without credentials");
        }
        Objects.requireNonNull(optionParameters, "optionParameters");
        if (optionParameters.size() > 20) throw new IllegalArgumentException("At most 20 preview options are allowed");
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        optionParameters.forEach((rawKey, rawValues) -> {
            String optionKey = text(rawKey, "option key", 64);
            if (!optionKey.matches("[A-Za-z][A-Za-z0-9_-]*")) throw new IllegalArgumentException("Invalid preview option key");
            List<String> values = new ArrayList<>(Objects.requireNonNull(rawValues, "option values").stream()
                    .map(value -> text(value, "option value", 256)).distinct().toList());
            if (values.isEmpty() || values.size() > 20) throw new IllegalArgumentException("Each preview option needs 1–20 values");
            if (normalized.putIfAbsent(optionKey, List.copyOf(values)) != null) throw new IllegalArgumentException("Duplicate preview option key");
        });
        optionParameters = Map.copyOf(normalized);
    }

    private static String text(String value, String field, int maximum) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty() || normalized.length() > maximum) {
            throw new IllegalArgumentException(field + " is blank or exceeds " + maximum + " characters");
        }
        return normalized;
    }
}
