package org.tavall.qa.request;

import org.tavall.qa.domain.TaskBoardDesignCandidate;
import java.util.List;
import java.util.UUID;

public record TaskBoardDesignReviewRegisterRequest(UUID taskId, List<TaskBoardDesignCandidate> candidates,
                                                   int expectedRevision) { }
