package org.tavall.qa.domain.execution;

import java.util.Objects;

/** Exact source and outstanding diff identity; the execution host verifies the actual workspace. */
public record TaskBoardExecutionSourceData(String repository, String branch, String commit, String diffSha256) {
    public TaskBoardExecutionSourceData {
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(branch, "branch");
        Objects.requireNonNull(commit, "commit");
        Objects.requireNonNull(diffSha256, "diffSha256");
        if (!repository.matches("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+") || repository.length() > 201) {
            throw new IllegalArgumentException("repository must be an owner/name identity");
        }
        if (branch.isBlank() || branch.length() > 256 || branch.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("branch must be bounded nonblank text");
        }
        if (!commit.matches("[a-f0-9]{40}") || !diffSha256.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("source and diff must have exact lowercase digests");
        }
    }
}
