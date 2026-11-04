package org.tavall.qa.request.external;

import org.tavall.qa.domain.external.TaskBoardExternalReferenceKey;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceSourceStatus;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Typed source observation attached to an already upserted QA task. */
public record TaskBoardExternalReferenceMutationRequest(
        UUID taskId,
        TaskBoardExternalReferenceKey key,
        String displayKey,
        String sourceUrl,
        TaskBoardExternalReferenceSourceStatus sourceStatus,
        String sourceRevision,
        String sourceCursor,
        String sourceFingerprint,
        Instant observedAt,
        long expectedSyncRevision
) {
    private static final Pattern SHA256 = Pattern.compile("^[a-fA-F0-9]{64}$");

    public TaskBoardExternalReferenceMutationRequest {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(key, "key");
        displayKey = requireText(displayKey, "displayKey", 255);
        sourceUrl = requireSourceUrl(sourceUrl);
        Objects.requireNonNull(sourceStatus, "sourceStatus");
        sourceRevision = requireText(sourceRevision, "sourceRevision", 255);
        sourceCursor = normalize(sourceCursor, 512);
        sourceFingerprint = requireText(sourceFingerprint, "sourceFingerprint", 128).toLowerCase(Locale.ROOT);
        if (!SHA256.matcher(sourceFingerprint).matches()) {
            throw new IllegalArgumentException("sourceFingerprint must be a SHA-256 hex digest");
        }
        Objects.requireNonNull(observedAt, "observedAt");
        if (expectedSyncRevision < 0) throw new IllegalArgumentException("expectedSyncRevision must not be negative");
    }

    private static String requireSourceUrl(String value) {
        String url = requireText(value, "sourceUrl", 2048);
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("sourceUrl must be a canonical HTTPS URL", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("sourceUrl must be HTTPS without credentials, query, or fragment");
        }
        return url;
    }

    private static String requireText(String value, String name, int maxLength) {
        Objects.requireNonNull(value, name);
        String trimmed = value.trim();
        if (trimmed.isBlank() || trimmed.length() > maxLength) {
            throw new IllegalArgumentException(name + " must contain 1.." + maxLength + " characters");
        }
        return trimmed;
    }

    private static String normalize(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException("External reference value is too long");
        return normalized;
    }
}
