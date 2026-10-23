package org.tavall.qa.request;

import org.tavall.qa.domain.auth.QaEnvironmentGrant;

import java.util.Objects;
import java.util.UUID;

public record TaskBoardEvidenceSubmissionRequest(
        UUID taskId,
        QaEnvironmentGrant environment,
        String buildReference,
        String testCase,
        String expectedResult,
        String actualResult,
        String evidenceReference,
        String publicSummary
) {
    public TaskBoardEvidenceSubmissionRequest {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(buildReference, "buildReference");
        Objects.requireNonNull(testCase, "testCase");
        Objects.requireNonNull(expectedResult, "expectedResult");
        Objects.requireNonNull(actualResult, "actualResult");
        Objects.requireNonNull(evidenceReference, "evidenceReference");
        publicSummary = publicSummary == null ? "" : publicSummary;
    }
}
