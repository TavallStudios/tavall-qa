package org.tavall.qa.cli.client;

import org.tavall.qa.domain.TaskBoardDesignCandidate;
import org.tavall.qa.domain.TaskBoardDesignReview;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Typed read model of a task's design review, including human decision and provisional AI review when present. */
public record QaCliDesignReviewSnapshot(
        UUID taskId,
        String surface,
        int revision,
        List<TaskBoardDesignCandidate> candidates,
        String winnerKey,
        String decidedByAccountId,
        String decidedAt,
        String note,
        String aiReviewerModel,
        String aiReviewerAccountId,
        String aiReviewerAgentIdentity,
        String aiReviewedAt,
        String aiCandidateKey,
        String aiRationale,
        List<String> aiEvidenceRefs,
        List<String> aiArtifactRefs,
        String aiScreenshotArtifactRef
) {
    public QaCliDesignReviewSnapshot {
        Objects.requireNonNull(taskId, "taskId");
        surface = Objects.requireNonNullElse(surface, "Unspecified web surface").trim();
        candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
        note = Objects.requireNonNullElse(note, "");
        aiEvidenceRefs = aiEvidenceRefs == null ? List.of() : List.copyOf(aiEvidenceRefs);
        aiArtifactRefs = aiArtifactRefs == null ? List.of() : List.copyOf(aiArtifactRefs);
    }

    public QaCliDesignReviewSnapshot(UUID taskId, String surface, int revision, List<TaskBoardDesignCandidate> candidates,
                                     String winnerKey, String decidedByAccountId, String decidedAt, String note) {
        this(taskId, surface, revision, candidates, winnerKey, decidedByAccountId, decidedAt, note,
                null, null, null, null, null, null, List.of(), List.of(), null);
    }

    public boolean decided() {
        return winnerKey != null;
    }

    public boolean aiReviewed() {
        return TaskBoardDesignReview.isApprovedAiReviewerModel(aiReviewerModel);
    }

    public boolean deployable() {
        return decided() || aiReviewed();
    }
}
