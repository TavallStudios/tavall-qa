package org.tavall.qa.cli.client;

import org.tavall.qa.domain.TaskBoardDesignCandidate;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Typed read model of a task's design review, including the attributed decision when present. */
public record QaCliDesignReviewSnapshot(
        UUID taskId,
        String surface,
        int revision,
        List<TaskBoardDesignCandidate> candidates,
        String winnerKey,
        String decidedByAccountId,
        String decidedAt,
        String note) {

    public QaCliDesignReviewSnapshot {
        Objects.requireNonNull(taskId, "taskId");
        surface = Objects.requireNonNullElse(surface, "Unspecified web surface").trim();
        candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
        note = Objects.requireNonNullElse(note, "");
    }

    public boolean decided() {
        return winnerKey != null;
    }
}
