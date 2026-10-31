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
        boolean canCancel,
        List<QaTaskAgentContextView> agentContexts
) {
    public QaTaskExecutionView {
        checkpoints = checkpoints == null ? List.of() : List.copyOf(checkpoints);
        evidenceLinks = evidenceLinks == null ? List.of() : List.copyOf(evidenceLinks);
        agentContexts = agentContexts == null ? List.of() : List.copyOf(agentContexts);
    }

    public QaTaskExecutionView(
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
        this(leaseHolder, providerId, runId, quotaRestriction, blockedReason, checkpoints, evidenceLinks, canResume, canCancel, List.of());
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
                List.of()
        );
    }

    public static QaTaskExecutionView of(
            String leaseHolder,
            String providerId,
            UUID runId,
            String quotaRestriction,
            String blockedReason,
            List<QaTaskExecutionCheckpointView> checkpoints,
            List<QaTaskExecutionEvidenceLink> evidenceLinks,
            List<QaTaskAgentContextView> agentContexts
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
                agentContexts
        );
    }

    public QaTaskExecutionView withAgentContexts(List<QaTaskAgentContextView> agentContexts) {
        return new QaTaskExecutionView(
                leaseHolder,
                providerId,
                runId,
                quotaRestriction,
                blockedReason,
                checkpoints,
                evidenceLinks,
                canResume,
                canCancel,
                agentContexts
        );
    }

    public boolean isBlocked() {
        return blockedReason != null && !blockedReason.isBlank();
    }

    public boolean hasQuotaRestriction() {
        return quotaRestriction != null && !quotaRestriction.isBlank();
    }
}
