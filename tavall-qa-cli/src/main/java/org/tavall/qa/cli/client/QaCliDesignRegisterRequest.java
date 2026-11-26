package org.tavall.qa.cli.client;

import org.tavall.qa.domain.TaskBoardDesignCandidate;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Typed request registering 2–4 live design candidates on a QA task. */
public record QaCliDesignRegisterRequest(
        UUID taskId,
        String surface,
        List<TaskBoardDesignCandidate> candidates,
        Integer expectedRevision,
        Path credentialFile) {

    public QaCliDesignRegisterRequest {
        Objects.requireNonNull(taskId, "taskId");
        surface = requireText(surface, "surface");
        candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates"));
        if (candidates.size() < 2 || candidates.size() > 4
                || candidates.stream().map(TaskBoardDesignCandidate::key).distinct().count() != candidates.size()) {
            throw new IllegalArgumentException("A design review requires 2–4 candidates with unique keys");
        }
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " is blank");
        }
        return normalized;
    }
}
