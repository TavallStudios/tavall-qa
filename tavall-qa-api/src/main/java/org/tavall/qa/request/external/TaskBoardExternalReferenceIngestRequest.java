package org.tavall.qa.request.external;

import org.tavall.qa.domain.external.TaskBoardExternalReferenceKey;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceProvider;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceSourceStatus;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Owner-supplied source observations and the QA scope for resolving or creating their task. */
public record TaskBoardExternalReferenceIngestRequest(
        String projectKey,
        String sectionKey,
        String initialTitle,
        Optional<UUID> existingTaskId,
        Observation github,
        Optional<Observation> linear
) {
    public TaskBoardExternalReferenceIngestRequest {
        projectKey = scopeKey(projectKey, 96);
        sectionKey = scopeKey(sectionKey, 128);
        initialTitle = Objects.requireNonNull(initialTitle, "initialTitle").trim();
        if (initialTitle.isEmpty() || initialTitle.length() > 220) {
            throw new IllegalArgumentException("initialTitle must contain 1..220 characters");
        }
        Objects.requireNonNull(existingTaskId, "existingTaskId");
        Objects.requireNonNull(github, "github");
        Objects.requireNonNull(linear, "linear");
        if (github.key().provider() != TaskBoardExternalReferenceProvider.GITHUB_ISSUE
                || linear.filter(value -> value.key().provider() != TaskBoardExternalReferenceProvider.LINEAR_ISSUE).isPresent()) {
            throw new IllegalArgumentException("An owner GitHub issue and at most one Linear issue are required");
        }
    }

    private static String scopeKey(String key, int maximumLength) {
        String normalized = Objects.requireNonNull(key, "scope key").trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > maximumLength || !normalized.matches("[a-z0-9][a-z0-9-]*")) {
            throw new IllegalArgumentException("Invalid QA scope key");
        }
        return normalized;
    }

    public record Observation(
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
        public Observation {
            Objects.requireNonNull(key, "key");
        }

        public TaskBoardExternalReferenceMutationRequest forTask(UUID taskId) {
            return new TaskBoardExternalReferenceMutationRequest(taskId, key, displayKey, sourceUrl,
                    sourceStatus, sourceRevision, sourceCursor, sourceFingerprint, observedAt, expectedSyncRevision);
        }
    }
}
