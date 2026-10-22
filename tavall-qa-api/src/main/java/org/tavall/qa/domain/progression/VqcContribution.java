package org.tavall.qa.domain.progression;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record VqcContribution(
        UUID contributionId,
        UUID taskId,
        UUID contributorMinecraftUuid,
        int baseVqc,
        int bonusVqc,
        boolean bonusApproved,
        String notes,
        Instant awardedAt
) {
    public VqcContribution {
        Objects.requireNonNull(contributionId, "contributionId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(contributorMinecraftUuid, "contributorMinecraftUuid");
        Objects.requireNonNull(awardedAt, "awardedAt");
        notes = notes == null ? "" : notes.trim();
    }

    public int totalVqc() {
        return baseVqc + (bonusApproved ? bonusVqc : 0);
    }
}
