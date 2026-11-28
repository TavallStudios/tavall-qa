package org.tavall.qa.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class TaskBoardDesignReviewTest {
    private final UUID actor = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-11-28T00:00:00Z");
    private final TaskBoardDesignReview review = TaskBoardDesignReview.register(UUID.randomUUID(), "Dashboard",
            List.of(new TaskBoardDesignCandidate("A", "Ledger", "https://example.org/a", "Editorial"),
                    new TaskBoardDesignCandidate("B", "Console", "https://example.org/b", "Operational")),
            1, actor, now);

    @Test
    void registrationAloneCannotDeployAndHumanCanDecideDirectly() {
        assertFalse(review.decided());
        assertFalse(review.aiReviewed());
        assertFalse(review.deployable());
        var decided = review.select("B", actor, "", now);
        assertTrue(decided.decided());
        assertFalse(decided.aiReviewed());
        assertTrue(decided.deployable());
    }

    @ParameterizedTest
    @ValueSource(strings = {"claude-opus-5-5", "claude-opus-4-6", "gemini-4-neon",
            "gemini-4-neon-high", "gemini-4-neon-medium", " GEMINI-4-NEON-High "})
    void approvedModelUnlocksProvisionalDeployability(String model) {
        var provisional = record(model, "a", List.of("evidence/1"));
        assertTrue(provisional.aiReviewed());
        assertTrue(provisional.deployable());
        assertFalse(provisional.decided());
        assertNull(provisional.winnerKey());
        assertNull(provisional.decidedByAccountId());
        assertEquals("A", provisional.aiCandidateKey());
        assertEquals(2, provisional.revision());
        var decided = provisional.select("B", actor, "Owner override", now);
        assertTrue(decided.decided());
        assertTrue(decided.deployable());
        assertEquals("B", decided.winnerKey());
        assertEquals("A", decided.aiCandidateKey());
        assertThrows(IllegalStateException.class, () -> decided.withAiReview(model, actor, "agent", now,
                "A", "Rationale", List.of("evidence/2"), List.of(), null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"gpt-6.1-terra", "claude-haiku-5-5", "deepai/glm-5.3-flash", "gemini-4-neonish"})
    void unapprovedModelCannotRecordReview(String model) {
        assertThrows(IllegalArgumentException.class, () -> record(model, "A", List.of("evidence/1")));
    }

    @Test
    void requiresRegisteredCandidateAndEvidence() {
        assertThrows(IllegalArgumentException.class, () -> record("claude-opus-5-5", "C", List.of("evidence/1")));
        assertThrows(IllegalArgumentException.class, () -> record("claude-opus-5-5", "A", List.of(" ")));
    }

    private TaskBoardDesignReview record(String model, String candidate, List<String> evidence) {
        return review.withAiReview(model, actor, "agent", now, candidate, "Rationale", evidence, List.of(), null);
    }
}
