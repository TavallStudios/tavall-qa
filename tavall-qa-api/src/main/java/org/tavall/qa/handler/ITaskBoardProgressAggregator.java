package org.tavall.qa.handler;

import org.tavall.qa.domain.TaskBoardTask;

import java.util.List;

public interface ITaskBoardProgressAggregator {
    int calculateWeightedPercent(List<TaskBoardTask> tasks);

    int calculateSectionProgress(List<TaskBoardTask> sectionTasks);
}
