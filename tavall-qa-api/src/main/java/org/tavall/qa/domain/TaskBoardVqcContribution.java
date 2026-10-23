package org.tavall.qa.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record TaskBoardVqcContribution(
        UUID contributionId,
        UUID taskId,
        Optional<UUID> contributorAccountId,
        Optional<UUID> contributorMinecraftUuid,
        int baseVqc,
        int bonusVqc,
        boolean bonusApproved,
        String notes,
        Instant awardedAt
) {
    public TaskBoardVqcContribution {
        Objects.requireNonNull(contributionId, "contributionId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(contributorAccountId, "contributorAccountId");
        Objects.requireNonNull(contributorMinecraftUuid, "contributorMinecraftUuid");
        if (baseVqc < 1) {
            throw new IllegalArgumentException("An accepted QA/QC task contributes at least one VQC");
        }
        if (bonusVqc < 0 || bonusVqc > 0 && !bonusApproved) {
            throw new IllegalArgumentException("Bonus VQC requires an authorized approval");
        }
        notes = notes == null ? "" : notes.trim();
        Objects.requireNonNull(awardedAt, "awardedAt");
    }
}
