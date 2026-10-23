package org.tavall.qa.domain;

import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaTier;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Private QA evidence with a deliberately separate public-safe summary. */
public record TaskBoardEvidence(
        UUID evidenceId,
        UUID taskId,
        UUID accountId,
        UUID submitterMinecraftUuid,
        Set<QaClass> qaClassesAtSubmission,
        Optional<QaTier> qaTierAtSubmission,
        QaEnvironmentGrant environment,
        String buildReference,
        String testCase,
        String expectedResult,
        String actualResult,
        String evidenceReference,
        String publicSummary,
        boolean publicSummaryApproved,
        String taskFingerprint,
        TaskBoardEvidenceStatus status,
        UUID reviewerAccountId,
        String reviewReason,
        Instant submittedAt,
        Instant reviewedAt
) {
    public TaskBoardEvidence {
        Objects.requireNonNull(evidenceId, "evidenceId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(accountId, "accountId");
        qaClassesAtSubmission = Set.copyOf(Objects.requireNonNull(qaClassesAtSubmission, "qaClassesAtSubmission"));
        Objects.requireNonNull(qaTierAtSubmission, "qaTierAtSubmission");
        Objects.requireNonNull(environment, "environment");
        buildReference = requireText(buildReference, "buildReference");
        testCase = requireText(testCase, "testCase");
        expectedResult = requireText(expectedResult, "expectedResult");
        actualResult = requireText(actualResult, "actualResult");
        evidenceReference = requireText(evidenceReference, "evidenceReference");
        publicSummary = publicSummary == null ? "" : publicSummary.trim();
        taskFingerprint = requireText(taskFingerprint, "taskFingerprint");
        Objects.requireNonNull(status, "status");
        reviewReason = reviewReason == null ? "" : reviewReason.trim();
        Objects.requireNonNull(submittedAt, "submittedAt");
        if (status == TaskBoardEvidenceStatus.PENDING_REVIEW
                && (reviewerAccountId != null || reviewedAt != null)) {
            throw new IllegalArgumentException("Pending evidence cannot contain review identity or time");
        }
        if (status != TaskBoardEvidenceStatus.PENDING_REVIEW
                && (reviewerAccountId == null || reviewedAt == null)) {
            throw new IllegalArgumentException("Reviewed evidence requires reviewer identity and time");
        }
        if (status == TaskBoardEvidenceStatus.REJECTED && reviewReason.isBlank()) {
            throw new IllegalArgumentException("Rejected evidence requires a staff reason");
        }
    }

    public boolean currentFor(String currentTaskFingerprint) {
        return taskFingerprint.equals(Objects.requireNonNull(currentTaskFingerprint, "currentTaskFingerprint"));
    }

    public TaskBoardEvidence review(
            TaskBoardEvidenceStatus result,
            UUID reviewerId,
            boolean publicSummaryApproved,
            String reason,
            Instant reviewedAt
    ) {
        if (status != TaskBoardEvidenceStatus.PENDING_REVIEW) {
            throw new IllegalStateException("Only pending evidence can be reviewed");
        }
        if (result == TaskBoardEvidenceStatus.PENDING_REVIEW) {
            throw new IllegalArgumentException("Evidence review must accept or reject the submission");
        }
        return new TaskBoardEvidence(
                evidenceId,
                taskId,
                accountId,
                submitterMinecraftUuid,
                qaClassesAtSubmission,
                qaTierAtSubmission,
                environment,
                buildReference,
                testCase,
                expectedResult,
                actualResult,
                evidenceReference,
                publicSummary,
                result == TaskBoardEvidenceStatus.ACCEPTED && publicSummaryApproved,
                taskFingerprint,
                result,
                Objects.requireNonNull(reviewerId, "reviewerId"),
                reason,
                submittedAt,
                Objects.requireNonNull(reviewedAt, "reviewedAt")
        );
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
