package org.tavall.qa.domain.external;

import java.util.Objects;
import java.util.Optional;

public record TaskBoardExternalReferenceMutationResult(
        Optional<TaskBoardExternalReference> reference,
        Optional<TaskBoardExternalReferenceConflict> conflict,
        TaskBoardExternalReferenceMutationOutcome outcome
) {
    public TaskBoardExternalReferenceMutationResult {
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(conflict, "conflict");
        Objects.requireNonNull(outcome, "outcome");
        if ((outcome == TaskBoardExternalReferenceMutationOutcome.CONFLICT) != conflict.isPresent()) {
            throw new IllegalArgumentException("Conflict outcome must include exactly one conflict entry");
        }
        if (outcome != TaskBoardExternalReferenceMutationOutcome.CONFLICT && reference.isEmpty()) {
            throw new IllegalArgumentException("Successful external reference mutation must return the reference");
        }
    }
}
