package org.tavall.qa.domain.execution;

import java.net.URI;
import java.util.Objects;

/** Exact context/evidence reference; its presence never grants permission to fetch or execute it. */
public record TaskBoardExecutionReferenceData(URI reference, String revision, String sha256) {
    public TaskBoardExecutionReferenceData {
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(revision, "revision");
        Objects.requireNonNull(sha256, "sha256");
        if (!reference.isAbsolute() || !java.util.List.of("https", "tavall-storage", "tavall-qa", "urn").contains(reference.getScheme())
                || reference.getUserInfo() != null || reference.toString().length() > 2048
                || revision.isBlank() || revision.length() > 256 || !sha256.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Context references require a bounded identity, revision and exact digest");
        }
    }
}
