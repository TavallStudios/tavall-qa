package org.tavall.qa.cli.client;

import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.util.Objects;
import java.util.UUID;

/** Typed upsert outcome echoing the idempotent task identity chosen by the QA board. */
public record QaCliTaskUpsertResult(
        UUID taskId,
        TaskBoardTaskStatus status,
        boolean created,
        boolean updated,
        String externalKey) {

    public QaCliTaskUpsertResult {
        Objects.requireNonNull(externalKey, "externalKey");
    }
}
