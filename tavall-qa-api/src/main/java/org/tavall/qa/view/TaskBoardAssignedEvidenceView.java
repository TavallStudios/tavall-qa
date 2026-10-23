package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardEvidenceStatus;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Task-scoped evidence content with Account and reviewer identities omitted. */
public record TaskBoardAssignedEvidenceView(
        UUID evidenceId,
        QaEnvironmentGrant environment,
        String buildReference,
        String testCase,
        String expectedResult,
        String actualResult,
        String evidenceReference,
        String publicSummary,
        TaskBoardEvidenceStatus status,
        boolean currentForTask,
        Instant submittedAt
) {
    public TaskBoardAssignedEvidenceView {
        Objects.requireNonNull(evidenceId, "evidenceId");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(buildReference, "buildReference");
        Objects.requireNonNull(testCase, "testCase");
        Objects.requireNonNull(expectedResult, "expectedResult");
        Objects.requireNonNull(actualResult, "actualResult");
        Objects.requireNonNull(evidenceReference, "evidenceReference");
        publicSummary = publicSummary == null ? "" : publicSummary;
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(submittedAt, "submittedAt");
    }
}
