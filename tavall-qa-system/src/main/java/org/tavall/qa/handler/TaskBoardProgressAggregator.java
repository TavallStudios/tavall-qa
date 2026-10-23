package org.tavall.qa.handler;

import org.springframework.stereotype.Component;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.util.List;

@Component
public final class TaskBoardProgressAggregator implements ITaskBoardProgressAggregator {
    @Override
    public int calculateWeightedPercent(List<TaskBoardTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        List<TaskBoardTask> eligibleTasks = tasks.stream()
                .filter(task -> task.status() != TaskBoardTaskStatus.CANCELLED)
                .toList();
        int totalWeight = eligibleTasks.stream().mapToInt(TaskBoardTask::weight).sum();
        int completedWeight = eligibleTasks.stream()
                .filter(TaskBoardTask::completed)
                .mapToInt(TaskBoardTask::weight)
                .sum();
        if (totalWeight <= 0) {
            return 0;
        }
        return (int) Math.round((completedWeight * 100.0D) / totalWeight);
    }

    @Override
    public int calculateSectionProgress(List<TaskBoardTask> sectionTasks) {
        return calculateWeightedPercent(sectionTasks);
    }
}
