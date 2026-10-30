package org.tavall.qa.domain.execution;

/** Execution observations do not accept a QA task or grant Human Verify. */
public enum TaskBoardExecutionStatus {
    QUEUED, RUNNING, COMPLETED, FAILED, TIMED_OUT, CANCELLED, BLOCKED, INFRASTRUCTURE_FAILURE
}
