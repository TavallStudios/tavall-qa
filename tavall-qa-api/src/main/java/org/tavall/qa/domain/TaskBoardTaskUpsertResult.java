package org.tavall.qa.domain;

import java.util.Objects;

/** Result of an atomic external-key task upsert. */
public record TaskBoardTaskUpsertResult(TaskBoardTask task, boolean created, boolean updated) {
    public TaskBoardTaskUpsertResult {
        Objects.requireNonNull(task, "task");
        if (created && updated) throw new IllegalArgumentException("A task cannot be created and updated together");
    }
}
