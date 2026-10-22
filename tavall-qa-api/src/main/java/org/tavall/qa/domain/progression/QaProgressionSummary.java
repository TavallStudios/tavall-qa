package org.tavall.qa.domain.progression;

import org.tavall.qa.domain.auth.QaTier;

import java.util.Objects;
import java.util.UUID;

public record QaProgressionSummary(
        UUID contributorMinecraftUuid,
        int totalVqc,
        int tasksAcceptedCount,
        int tasksRejectedCount,
        int acceptanceRatePercent,
        QaTier currentTier,
        QaTier eligibleTier,
        boolean promotionApproved
) {
    public QaProgressionSummary {
        Objects.requireNonNull(contributorMinecraftUuid, "contributorMinecraftUuid");
        Objects.requireNonNull(currentTier, "currentTier");
        Objects.requireNonNull(eligibleTier, "eligibleTier");
    }

    public static QaTier calculateEligibleTier(int vqcCount) {
        if (vqcCount >= 100) return QaTier.TIER_V;
        if (vqcCount >= 50) return QaTier.TIER_IV;
        if (vqcCount >= 25) return QaTier.TIER_III;
        if (vqcCount >= 10) return QaTier.TIER_II;
        if (vqcCount >= 3) return QaTier.TIER_I;
        return QaTier.TIER_0;
    }
}
