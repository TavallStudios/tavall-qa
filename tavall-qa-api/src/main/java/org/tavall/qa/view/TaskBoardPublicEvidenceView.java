package org.tavall.qa.view;

import org.tavall.qa.domain.auth.QaEnvironmentGrant;

import java.time.Instant;
import java.util.Objects;

/** Sanitized evidence summary approved for the public projection. */
public record TaskBoardPublicEvidenceView(
        String summary,
        QaEnvironmentGrant environment,
        Instant submittedAt
) {
    public TaskBoardPublicEvidenceView {
        Objects.requireNonNull(summary, "summary");
        summary = summary.trim();
        if (summary.isBlank()) {
            throw new IllegalArgumentException("Public evidence summary must not be blank");
        }
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(submittedAt, "submittedAt");
    }
}
