package org.tavall.qa.request;

import org.tavall.qa.domain.TaskBoardDesignCandidate;
import java.util.List;
import java.util.UUID;

public record TaskBoardDesignReviewRegisterRequest(UUID taskId, String surface,
                                                   List<TaskBoardDesignCandidate> candidates,
                                                   int expectedRevision) {
    public TaskBoardDesignReviewRegisterRequest(UUID taskId, List<TaskBoardDesignCandidate> candidates, int expectedRevision) {
        this(taskId, "Unspecified web surface", candidates, expectedRevision);
    }
}
