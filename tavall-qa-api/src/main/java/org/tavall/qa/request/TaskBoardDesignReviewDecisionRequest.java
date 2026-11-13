package org.tavall.qa.request;

import java.util.UUID;

public record TaskBoardDesignReviewDecisionRequest(UUID taskId, String candidateKey, String note,
                                                   int expectedRevision) { }
