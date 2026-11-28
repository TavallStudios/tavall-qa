package org.tavall.qa.domain.external;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** An ambiguous identity has no resolved task ID. Provider conflicts retain the resolved QA identity. */
public record TaskBoardExternalReferenceIngestResult(
        Optional<UUID> taskId,
        boolean createdTask,
        List<TaskBoardExternalReferenceMutationResult> observations
) {
    public TaskBoardExternalReferenceIngestResult {
        Objects.requireNonNull(taskId, "taskId");
        observations = List.copyOf(observations);
    }
}
