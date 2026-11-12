package org.tavall.qa.domain;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

/** One named live design direction. URLs are HTTPS documents, never executable schemes. */
public record TaskBoardDesignCandidate(String key, String name, String previewUrl, String rationale) {
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
    }

    private static String text(String value, String field, int maximum) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty() || normalized.length() > maximum) {
            throw new IllegalArgumentException(field + " is blank or exceeds " + maximum + " characters");
        }
        return normalized;
    }
}
