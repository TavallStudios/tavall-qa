package org.tavall.qa.web.client;

import org.tavall.qa.web.pages.task.QaTaskExecutionView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Client for retrieving QA task execution state (lease and checkpoint history)
 * from the QA execution backend (/api/execution/tasks/{taskId}/...).
 */
public interface QaTaskExecutionClient {

    Optional<QaExecutionLeaseResponse> fetchLease(UUID taskId);

    List<QaExecutionCheckpointResponse> fetchCheckpoints(UUID taskId, UUID runId);

    /**
     * Populates the {@link QaTaskExecutionView} from backend lease and checkpoint data
     * for authorized staff only. If the caller is not authorized staff, returns {@link Optional#empty()}
     * so public views remain completely free of execution data.
     */
    default Optional<QaTaskExecutionView> populateExecutionView(UUID taskId, boolean isStaff) {
        if (!isStaff || taskId == null) {
            return Optional.empty();
        }
        Optional<QaExecutionLeaseResponse> leaseOpt = fetchLease(taskId);
        if (leaseOpt.isEmpty()) {
            return Optional.empty();
        }
        QaExecutionLeaseResponse lease = leaseOpt.get();
        List<QaExecutionCheckpointResponse> checkpoints = fetchCheckpoints(taskId, lease.runId());
        return Optional.of(QaTaskExecutionView.from(lease, checkpoints));
    }
}
