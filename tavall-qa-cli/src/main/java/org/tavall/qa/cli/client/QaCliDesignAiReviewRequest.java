package org.tavall.qa.cli.client;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Typed request recording a provisional AI design review through the QA execution API. */
public record QaCliDesignAiReviewRequest(
        UUID taskId,
        String candidateKey,
        String rationale,
        List<String> evidenceRefs,
        List<String> artifactRefs,
        String screenshotArtifactRef,
        String reviewerModel,
        String reviewerAgentIdentity,
        Integer expectedRevision,
        Path credentialFile
) {
    public QaCliDesignAiReviewRequest {
        Objects.requireNonNull(taskId, "taskId");
        candidateKey = candidateKey == null ? "" : candidateKey.trim();
        rationale = rationale == null ? "" : rationale.trim();
        evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        artifactRefs = artifactRefs == null ? List.of() : List.copyOf(artifactRefs);
        screenshotArtifactRef = screenshotArtifactRef == null ? "" : screenshotArtifactRef.trim();
        reviewerModel = reviewerModel == null ? "" : reviewerModel.trim();
        reviewerAgentIdentity = reviewerAgentIdentity == null ? "" : reviewerAgentIdentity.trim();
    }
}
