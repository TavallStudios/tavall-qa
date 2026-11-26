package org.tavall.qa.cli.client;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 * Typed contract for the QA execution API used by the canonical {@code qa} CLI commands.
 * Implementations exchange the agent credential for a short-lived token and never expose secrets.
 */
public interface IQaExecutionApiClient {

    /** Upserts a task by external key; re-sending the same body keeps the same task id. */
    QaCliTaskUpsertResult upsertTask(QaCliTaskUpsertRequest request);

    /**
     * Reads the design review attached to a task; empty when the task has no review (HTTP 404).
     */
    Optional<QaCliDesignReviewSnapshot> getDesignReview(UUID taskId, Path credentialFile);

    /**
     * Registers 2–4 live design candidates; defaults {@code expectedRevision} to the current
     * review revision (or 0 when no review exists yet).
     */
    QaCliDesignRegisterResult registerDesignCandidates(QaCliDesignRegisterRequest request);
}
