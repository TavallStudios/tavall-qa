package org.tavall.qa.web.pages.task;

import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointData;
import org.tavall.qa.domain.execution.TaskBoardExecutionLeaseData;

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
        boolean canRetry,
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
                true,
                true
        );
    }

    public static QaTaskExecutionView from(
            TaskBoardExecutionLeaseData lease,
            List<TaskBoardExecutionCheckpointData> checkpoints,
            String quotaRestriction,
            String blockedReason,
            List<QaTaskExecutionEvidenceLink> evidenceLinks,
            boolean canResume,
            boolean canRetry,
            boolean canCancel
    ) {
        String leaseHolder = lease != null ? lease.ownerAccountId().toString() : null;
        UUID runId = lease != null ? lease.runId() : null;
        String providerId = null;
        List<QaTaskExecutionCheckpointView> checkpointViews = List.of();
        if (checkpoints != null && !checkpoints.isEmpty()) {
            List<TaskBoardExecutionCheckpointData> sorted = checkpoints.stream()
                    .sorted(Comparator.comparingLong(TaskBoardExecutionCheckpointData::sequence))
                    .toList();
            checkpointViews = sorted.stream()
                    .map(cp -> new QaTaskExecutionCheckpointView(
                            cp.checkpointId(),
                            cp.sequence(),
                            cp.providerId(),
                            cp.status().name(),
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
                canRetry,
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
