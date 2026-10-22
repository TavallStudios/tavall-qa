package org.tavall.qa.handler;

import org.tavall.qa.domain.TaskBoardActionType;

public interface ITaskBoardActionTargetResolver {
    String normalizeTarget(TaskBoardActionType type, String target);

    String resolveHref(TaskBoardActionType type, String target);
}
