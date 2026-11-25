package org.tavall.qa.handler;

import org.tavall.qa.domain.TaskBoardVenture;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.auth.QaSubjectContext;

import java.util.List;
import java.util.Optional;

/** Read-only venture directory contract, separate from task board projects. */
public interface ITaskBoardVentureQueryHandler {
    List<TaskBoardVenture> findVentures(QaSubjectContext subject);

    Optional<TaskBoardVenture> findVenture(String slug, QaSubjectContext subject);

    /** Tasks matched to a venture's owner repositories while remaining on tavall-engineering. */
    List<TaskBoardTask> findVentureTasks(String slug, QaSubjectContext subject);
}
