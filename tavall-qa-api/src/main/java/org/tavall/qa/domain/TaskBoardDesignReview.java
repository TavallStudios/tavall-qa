package org.tavall.qa.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Task-owned design candidates and an immutable, attributed reviewer decision. */
public record TaskBoardDesignReview(UUID taskId, List<TaskBoardDesignCandidate> candidates, int revision,
                                    UUID registeredByAccountId, Instant registeredAt, String winnerKey,
                                    UUID decidedByAccountId, Instant decidedAt, String note) {
    public TaskBoardDesignReview {
        Objects.requireNonNull(taskId, "taskId");
        candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
        if (candidates.size() < 2 || candidates.size() > 4
                || candidates.stream().map(TaskBoardDesignCandidate::key).distinct().count() != candidates.size()) {
            throw new IllegalArgumentException("A design review requires 2–4 candidates with unique keys");
        }
        if (revision < 1) throw new IllegalArgumentException("Review revision must be positive");
        Objects.requireNonNull(registeredByAccountId, "registeredByAccountId");
        Objects.requireNonNull(registeredAt, "registeredAt");
        note = note == null ? "" : note.trim();
        if (note.length() > 4000) throw new IllegalArgumentException("Decision note exceeds 4000 characters");
        if (winnerKey == null) {
            if (decidedByAccountId != null || decidedAt != null || !note.isEmpty()) {
                throw new IllegalArgumentException("Only decided reviews may carry decision details");
            }
        } else {
            String winner = winnerKey;
            if (candidates.stream().noneMatch(candidate -> candidate.key().equals(winner))) {
                throw new IllegalArgumentException("Winner must be a registered candidate");
            }
            Objects.requireNonNull(decidedByAccountId, "decidedByAccountId");
            Objects.requireNonNull(decidedAt, "decidedAt");
        }
    }

    public boolean decided() { return winnerKey != null; }

    public static TaskBoardDesignReview register(UUID taskId, List<TaskBoardDesignCandidate> candidates,
                                                int revision, UUID actor, Instant now) {
        return new TaskBoardDesignReview(taskId, candidates, revision, actor, now, null, null, null, "");
    }

    public TaskBoardDesignReview select(String key, UUID actor, String decisionNote, Instant now) {
        return new TaskBoardDesignReview(taskId, candidates, revision + 1, registeredByAccountId, registeredAt,
                key, actor, now, decisionNote);
    }
}
