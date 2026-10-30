package org.tavall.qa.domain.mirror;

import java.time.Instant;
import java.util.Objects;

/**
 * Typed, versioned record envelope representation for state redundancy files.
 */
public record TaskBoardFileMirrorRecord<T>(
        int schemaVersion,
        String ownerIdentity,
        String recordType,
        String recordId,
        long revision,
        Instant updatedAt,
        String payloadSha256,
        T payload
) {
    public static final String OWNER_IDENTITY = "qa";
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public TaskBoardFileMirrorRecord {
        Objects.requireNonNull(ownerIdentity, "ownerIdentity");
        Objects.requireNonNull(recordType, "recordType");
        Objects.requireNonNull(recordId, "recordId");
        Objects.requireNonNull(updatedAt, "updatedAt");
        Objects.requireNonNull(payloadSha256, "payloadSha256");
        Objects.requireNonNull(payload, "payload");
    }

    public TaskBoardFileMirrorRecord(
            int schemaVersion,
            String ownerIdentity,
            String recordType,
            String recordId,
            long revision,
            Instant updatedAt,
            T payload
    ) {
        this(schemaVersion, ownerIdentity, recordType, recordId, revision, updatedAt, "", payload);
    }

    public static <T> TaskBoardFileMirrorRecord<T> of(
            String recordType,
            String recordId,
            long revision,
            Instant updatedAt,
            String payloadSha256,
            T payload
    ) {
        return new TaskBoardFileMirrorRecord<>(
                CURRENT_SCHEMA_VERSION,
                OWNER_IDENTITY,
                recordType,
                recordId,
                revision,
                updatedAt,
                payloadSha256,
                payload
        );
    }

    public static <T> TaskBoardFileMirrorRecord<T> of(
            String recordType,
            String recordId,
            long revision,
            Instant updatedAt,
            T payload
    ) {
        return of(recordType, recordId, revision, updatedAt, "", payload);
    }
}
