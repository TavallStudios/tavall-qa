package org.tavall.qa.web.pages.task;

import org.tavall.qa.web.client.QaExecutionCheckpointResponse;
import org.tavall.qa.web.client.QaExecutionLeaseResponse;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Execution details and action controls for a QA task.
 * Internal details are only rendered to authorized contributors, QA reviewers, and administrators.
 */
public record QaTaskExecutionView(
        String leaseHolder,
        String providerId,
        UUID runId,
        String quotaRestriction,
        String blockedReason,
        List<QaTaskExecutionCheckpointView> checkpoints,
        List<QaTaskExecutionEvidenceLink> evidenceLinks,
        boolean canResume,
        boolean canCancel
) {
    public QaTaskExecutionView {
        checkpoints = checkpoints == null ? List.of() : List.copyOf(checkpoints);
        evidenceLinks = evidenceLinks == null ? List.of() : List.copyOf(evidenceLinks);
    }

    public static QaTaskExecutionView of(
            String leaseHolder,
            String providerId,
            UUID runId,
            String quotaRestriction,
            String blockedReason,
            List<QaTaskExecutionCheckpointView> checkpoints,
            List<QaTaskExecutionEvidenceLink> evidenceLinks
    ) {
        return new QaTaskExecutionView(
                leaseHolder,
                providerId,
                runId,
                quotaRestriction,
                blockedReason,
                checkpoints,
                evidenceLinks,
                true,
                true
        );
    }

    public static QaTaskExecutionView from(
            QaExecutionLeaseResponse lease,
            List<QaExecutionCheckpointResponse> checkpoints
    ) {
        return from(lease, checkpoints, null, null, List.of(), true, true);
    }

    public static QaTaskExecutionView from(
            QaExecutionLeaseResponse lease,
            List<QaExecutionCheckpointResponse> checkpoints,
            String quotaRestriction,
            String blockedReason,
            List<QaTaskExecutionEvidenceLink> evidenceLinks,
            boolean canResume,
            boolean canCancel
    ) {
        String leaseHolder = lease != null && lease.ownerAccountId() != null ? lease.ownerAccountId().toString() : null;
        UUID runId = lease != null ? lease.runId() : null;
        String providerId = null;
        List<QaTaskExecutionCheckpointView> checkpointViews = List.of();
        if (checkpoints != null && !checkpoints.isEmpty()) {
            List<QaExecutionCheckpointResponse> sorted = checkpoints.stream()
                    .sorted(Comparator.comparingLong(QaExecutionCheckpointResponse::sequence))
                    .toList();
            checkpointViews = sorted.stream()
                    .map(cp -> new QaTaskExecutionCheckpointView(
                            cp.checkpointId(),
                            cp.sequence(),
                            cp.providerId(),
                            cp.status() != null ? cp.status() : "UNKNOWN",
                            cp.recordedAt(),
                            ""
                    ))
                    .toList();
            providerId = sorted.getLast().providerId();
        }
        return new QaTaskExecutionView(
                leaseHolder,
                providerId,
                runId,
                quotaRestriction,
                blockedReason,
                checkpointViews,
                evidenceLinks,
                canResume,
                canCancel
        );
    }

    public boolean isBlocked() {
        return blockedReason != null && !blockedReason.isBlank();
    }

    public boolean hasQuotaRestriction() {
        return quotaRestriction != null && !quotaRestriction.isBlank();
    }
}
