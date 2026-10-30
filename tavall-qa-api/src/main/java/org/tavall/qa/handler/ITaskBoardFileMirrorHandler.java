package org.tavall.qa.handler;

import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointData;
import org.tavall.qa.domain.execution.TaskBoardExecutionLeaseData;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorDrift;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorDriftType;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorReconcileResult;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorVerificationReport;

import java.nio.file.Path;
import java.util.List;

/**
 * File redundancy mirror handler for PostgreSQL-authoritative Task Board state.
 * Writes typed versioned records after PostgreSQL commits, never serves normal request paths,
 * provides explicit verify/reconcile operations, and quarantines torn or invalid files.
 */
public interface ITaskBoardFileMirrorHandler {
    void mirrorTask(TaskBoardTask task);

    void mirrorExecutionLease(TaskBoardExecutionLeaseData lease);

    void mirrorExecutionCheckpoint(TaskBoardExecutionCheckpointData checkpoint);

    TaskBoardFileMirrorVerificationReport verify();

    TaskBoardFileMirrorReconcileResult reconcile();

    void recordDrift(String recordType, String recordId, TaskBoardFileMirrorDriftType driftType, String details);

    List<String> recoveryReports();

    List<TaskBoardFileMirrorDrift> recordedDrifts();

    Path mirrorRoot();
}
