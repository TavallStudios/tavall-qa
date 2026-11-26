package org.tavall.qa.cli.client;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Typed outcome of registering design candidates on a QA task. */
public record QaCliDesignRegisterResult(
        UUID taskId,
        int revision,
        List<String> candidateKeys,
        String winnerKey,
        boolean decided) {

    public QaCliDesignRegisterResult {
        Objects.requireNonNull(taskId, "taskId");
        candidateKeys = List.copyOf(Objects.requireNonNull(candidateKeys, "candidateKeys"));
    }
}
