package org.tavall.qa.domain.external;

/** Normalized observation of an external record; it never changes QA execution status by itself. */
public enum TaskBoardExternalReferenceSourceStatus {
    UNKNOWN,
    OPEN,
    READY,
    ACTIVE,
    IN_PROGRESS,
    BLOCKED,
    DONE,
    COMPLETED,
    CLOSED,
    CANCELLED,
    ARCHIVED;

    public boolean indicatesActiveWork() {
        return switch (this) {
            case OPEN, READY, ACTIVE, IN_PROGRESS, BLOCKED -> true;
            default -> false;
        };
    }

    public boolean indicatesTerminalWork() {
        return switch (this) {
            case DONE, COMPLETED, CLOSED, CANCELLED, ARCHIVED -> true;
            default -> false;
        };
    }
}
