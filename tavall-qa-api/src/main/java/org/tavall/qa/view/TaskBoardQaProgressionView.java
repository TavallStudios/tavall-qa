package org.tavall.qa.view;

import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaTier;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Authorized contribution projection; VQC is not a promotion or permission grant. */
public record TaskBoardQaProgressionView(
        UUID accountId,
        Set<QaClass> qaClasses,
        Optional<QaTier> currentTier,
        int acceptedQaQcTaskCount,
        int vqcEarned,
        Optional<Integer> nextTierVqcGuideline
) {
    public TaskBoardQaProgressionView {
        Objects.requireNonNull(accountId, "accountId");
        qaClasses = Set.copyOf(Objects.requireNonNull(qaClasses, "qaClasses"));
        Objects.requireNonNull(currentTier, "currentTier");
        if (acceptedQaQcTaskCount < 0 || vqcEarned < 0) {
            throw new IllegalArgumentException("QA progression counts cannot be negative");
        }
        Objects.requireNonNull(nextTierVqcGuideline, "nextTierVqcGuideline");
    }
}
