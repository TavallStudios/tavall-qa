package org.tavall.qa.domain.external;

import java.util.Objects;
import java.util.UUID;

/** Stable external identity based on immutable provider identifiers, never a title or URL slug. */
public record TaskBoardExternalReferenceKey(
        TaskBoardExternalReferenceProvider provider,
        String sourceNamespaceId,
        String sourceId
) {
    public TaskBoardExternalReferenceKey {
        Objects.requireNonNull(provider, "provider");
        sourceNamespaceId = requireText(sourceNamespaceId, "sourceNamespaceId", 255);
        sourceId = requireText(sourceId, "sourceId", 255);
        if (provider == TaskBoardExternalReferenceProvider.GITHUB_ISSUE
                && (!sourceNamespaceId.matches("0|[1-9][0-9]*") || !sourceId.matches("0|[1-9][0-9]*"))) {
            throw new IllegalArgumentException("GitHub reference identity requires numeric repository and issue IDs");
        }
        if (provider == TaskBoardExternalReferenceProvider.LINEAR_ISSUE) {
            sourceNamespaceId = canonicalUuid(sourceNamespaceId, "Linear workspace ID");
            sourceId = canonicalUuid(sourceId, "Linear issue ID");
        }
    }

    public String stableKey() {
        return provider.name().toLowerCase(java.util.Locale.ROOT) + ":" + sourceNamespaceId + ":" + sourceId;
    }

    public String advisoryLockKey() {
        return "tavall-qa:external-reference:" + stableKey();
    }

    private static String requireText(String value, String name, int maximumLength) {
        Objects.requireNonNull(value, name);
        String trimmed = value.trim();
        if (trimmed.isBlank() || trimmed.length() > maximumLength) {
            throw new IllegalArgumentException(name + " must contain 1.." + maximumLength + " characters");
        }
        return trimmed;
    }

    private static String canonicalUuid(String value, String name) {
        try {
            String canonical = UUID.fromString(value).toString();
            if (!canonical.equalsIgnoreCase(value)) {
                throw new IllegalArgumentException(name + " must be a canonical UUID");
            }
            return canonical;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(name + " must be a UUID", exception);
        }
    }
}
