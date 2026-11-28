package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardDesignCandidate;
import org.tavall.qa.domain.TaskBoardDesignReview;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskBoardDesignReviewEntityTest {
    @Test
    void mapsProvisionalAttributionAndHumanDecisionIndependently() {
        Instant now = Instant.parse("2026-11-28T00:00:00Z");
        UUID owner = UUID.randomUUID();
        var pending = TaskBoardDesignReview.register(UUID.randomUUID(), "Dashboard",
                List.of(new TaskBoardDesignCandidate("A", "Ledger", "https://example.org/a", "Editorial"),
                        new TaskBoardDesignCandidate("B", "Console", "https://example.org/b", "Operational")),
                1, owner, now);
        var provisional = pending.withAiReview("gemini-4-neon-medium", UUID.randomUUID(), "review-agent",
                now, "A", "Hierarchy", List.of("evidence/1"), List.of("artifact/1"), "screenshot/1");
        var decided = provisional.select("B", owner, "Owner decision", now);

        for (var review : List.of(pending, provisional, decided)) {
            assertEquals(review, TaskBoardDesignReviewEntity.fromDomain(review).toDomain());
        }
    }
}
