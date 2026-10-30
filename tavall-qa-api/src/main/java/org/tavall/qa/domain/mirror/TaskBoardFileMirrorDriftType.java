package org.tavall.qa.domain.mirror;

/**
 * Disagreement or fault categories between PostgreSQL authority and file mirror.
 */
public enum TaskBoardFileMirrorDriftType {
    MISSING_IN_MIRROR,
    MISSING_IN_POSTGRES,
    CONTENT_MISMATCH,
    CORRUPT_OR_TORN
}
