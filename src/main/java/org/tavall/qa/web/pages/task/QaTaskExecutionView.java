package org.tavall.qa.web.pages.task;

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

    public boolean isBlocked() {
        return blockedReason != null && !blockedReason.isBlank();
    }

    public boolean hasQuotaRestriction() {
        return quotaRestriction != null && !quotaRestriction.isBlank();
    }
}
