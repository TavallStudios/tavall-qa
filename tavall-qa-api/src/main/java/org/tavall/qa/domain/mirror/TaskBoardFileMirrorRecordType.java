package org.tavall.qa.domain.mirror;

import java.util.Objects;

/**
 * Record types supported by the QA file redundancy mirror.
 */
public enum TaskBoardFileMirrorRecordType {
    TASK("task"),
    EXECUTION_LEASE("execution-lease"),
    CHECKPOINT("checkpoint");

    private final String directoryName;

    TaskBoardFileMirrorRecordType(String directoryName) {
        this.directoryName = directoryName;
    }

    public String directoryName() {
        return directoryName;
    }

    public static TaskBoardFileMirrorRecordType fromDirectoryName(String name) {
        Objects.requireNonNull(name, "name");
        for (TaskBoardFileMirrorRecordType type : values()) {
            if (type.directoryName.equalsIgnoreCase(name.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown record type directory: " + name);
    }
}
