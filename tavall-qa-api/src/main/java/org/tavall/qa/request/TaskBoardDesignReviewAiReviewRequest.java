package org.tavall.qa.request;

import java.util.List;
import java.util.UUID;

/** Typed request recording a provisional AI design review on a task-owned review aggregate. */
public record TaskBoardDesignReviewAiReviewRequest(
        UUID taskId,
        String candidateKey,
        String rationale,
        List<String> evidenceRefs,
        List<String> artifactRefs,
        String screenshotArtifactRef,
        String reviewerModel,
        String reviewerAgentIdentity,
        int expectedRevision
) {
    public TaskBoardDesignReviewAiReviewRequest {
        evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        artifactRefs = artifactRefs == null ? List.of() : List.copyOf(artifactRefs);
    }
}
