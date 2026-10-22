package org.tavall.qa.handler;

import org.springframework.stereotype.Component;
import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.view.TaskBoardColumnView;
import org.tavall.qa.view.TaskBoardDashboardView;
import org.tavall.qa.view.TaskBoardProjectSummaryView;
import org.tavall.qa.view.TaskBoardProjectView;
import org.tavall.qa.view.TaskBoardPublicActivityView;
import org.tavall.qa.view.TaskBoardPublicDashboardView;
import org.tavall.qa.view.TaskBoardPublicProjectSummaryView;
import org.tavall.qa.view.TaskBoardPublicProjectView;
import org.tavall.qa.view.TaskBoardPublicSectionView;
import org.tavall.qa.view.TaskBoardPublicTaskView;
import org.tavall.qa.view.TaskBoardResolvedActionView;
import org.tavall.qa.view.TaskBoardTaskView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public final class TaskBoardQueryHandler implements ITaskBoardQueryHandler {
    private final ITaskBoardDataHandler dataHandler;
    private final ITaskBoardActionTargetResolver actionTargetResolver;

    public TaskBoardQueryHandler(
            ITaskBoardDataHandler dataHandler,
            ITaskBoardActionTargetResolver actionTargetResolver
    ) {
        this.dataHandler = Objects.requireNonNull(dataHandler, "dataHandler");
        this.actionTargetResolver = Objects.requireNonNull(
                actionTargetResolver,
                "actionTargetResolver"
        );
    }

    @Override
    public TaskBoardDashboardView dashboard() {
        if (!dataHandler.isAvailable()) {
            return new TaskBoardDashboardView(false, List.of());
        }
        List<TaskBoardProjectSummaryView> projects = dataHandler.findProjects().stream()
                .map(this::summary)
                .toList();
        return new TaskBoardDashboardView(true, projects);
    }

    @Override
    public TaskBoardPublicDashboardView publicDashboard() {
        if (!dataHandler.isAvailable()) {
            return new TaskBoardPublicDashboardView(
                    false, List.of(), List.of(), 0, 0, 0, 0, 0, List.of(), List.of()
            );
        }
        List<TaskBoardProject> publicProjects = dataHandler.findProjects().stream()
                .filter(TaskBoardProject::publicVisible)
                .toList();
        List<TaskBoardPublicProjectView> projectBoards = publicProjects.stream()
                .map(project -> {
                    List<TaskBoardTask> tasks = publicTasks(project.projectKey());
                    return new TaskBoardPublicProjectView(
                            publicSummary(project, tasks),
                            publicSections(tasks),
                            publicHighlights(tasks)
                    );
                })
                .toList();
        List<TaskBoardPublicProjectSummaryView> projects = projectBoards.stream()
                .map(TaskBoardPublicProjectView::summary)
                .toList();
        List<PublicTaskEntry> entries = new ArrayList<>();
        for (TaskBoardProject project : publicProjects) {
            for (TaskBoardTask task : publicTasks(project.projectKey())) {
                entries.add(new PublicTaskEntry(project, task));
            }
        }
        List<TaskBoardTask> visibleTasks = entries.stream().map(PublicTaskEntry::task).toList();
        int activeCount = (int) entries.stream()
                .filter(entry -> entry.task().status() == TaskBoardTaskStatus.ACTIVE
                        || entry.task().status() == TaskBoardTaskStatus.REVIEW)
                .count();
        List<TaskBoardPublicActivityView> activeTasks = entries.stream()
                .filter(entry -> entry.task().status() == TaskBoardTaskStatus.ACTIVE
                        || entry.task().status() == TaskBoardTaskStatus.REVIEW)
                .sorted(Comparator.comparing((PublicTaskEntry entry) -> entry.task().updatedAt()).reversed())
                .limit(6)
                .map(this::publicActivity)
                .toList();
        List<TaskBoardPublicActivityView> recentlyCompleted = entries.stream()
                .filter(entry -> entry.task().completed())
                .sorted(Comparator.comparing((PublicTaskEntry entry) -> entry.task().updatedAt()).reversed())
                .limit(6)
                .map(this::publicActivity)
                .toList();
        int completedCount = (int) visibleTasks.stream().filter(TaskBoardTask::completed).count();
        int nhvCount = (int) visibleTasks.stream()
                .filter(task -> task.qaStatus() != TaskBoardQaStatus.HUMAN_VERIFIED)
                .count();
        return new TaskBoardPublicDashboardView(
                true,
                projects,
                projectBoards,
                weightedPercent(visibleTasks),
                visibleTasks.size(),
                activeCount,
                completedCount,
                nhvCount,
                activeTasks,
                recentlyCompleted
        );
    }

    @Override
    public Optional<TaskBoardProjectView> findProjectView(String projectKey) {
        if (!dataHandler.isAvailable()) {
            return Optional.empty();
        }
        Optional<TaskBoardProject> projectOptional = dataHandler.findProject(projectKey);
        if (projectOptional.isEmpty()) {
            return Optional.empty();
        }
        List<TaskBoardTask> tasks = dataHandler.findTasks(projectKey);
        List<TaskBoardColumnView> columns = Arrays.stream(TaskBoardTaskStatus.values())
                .map(status -> new TaskBoardColumnView(
                        status,
                        tasks.stream().filter(task -> task.status() == status).toList()
                ))
                .toList();
        int nhvCount = (int) tasks.stream()
                .filter(task -> task.qaStatus() != TaskBoardQaStatus.HUMAN_VERIFIED)
                .count();
        int hostRequiredCount = (int) tasks.stream()
                .filter(TaskBoardTask::hostValidationRequired)
                .count();
        int blockedCount = (int) tasks.stream()
                .filter(task -> task.status() == TaskBoardTaskStatus.BLOCKED)
                .count();
        int verifiedCount = (int) tasks.stream()
                .filter(TaskBoardTask::humanVerified)
                .count();
        return Optional.of(new TaskBoardProjectView(
                projectOptional.get(),
                columns,
                tasks.size(),
                nhvCount,
                hostRequiredCount,
                blockedCount,
                verifiedCount
        ));
    }

    @Override
    public Optional<TaskBoardPublicProjectView> findPublicProjectView(String projectKey) {
        if (!dataHandler.isAvailable()) {
            return Optional.empty();
        }
        Optional<TaskBoardProject> projectOptional = dataHandler.findProject(projectKey)
                .filter(TaskBoardProject::publicVisible);
        if (projectOptional.isEmpty()) {
            return Optional.empty();
        }
        TaskBoardProject project = projectOptional.get();
        List<TaskBoardTask> publicTasks = publicTasks(project.projectKey());
        return Optional.of(new TaskBoardPublicProjectView(
                publicSummary(project, publicTasks),
                publicSections(publicTasks),
                publicHighlights(publicTasks)
        ));
    }

    @Override
    public Optional<TaskBoardPublicTaskView> findPublicTaskView(UUID taskId) {
        if (!dataHandler.isAvailable()) {
            return Optional.empty();
        }
        return dataHandler.findTask(taskId)
                .filter(TaskBoardTask::publicVisible)
                .filter(task -> dataHandler.findProject(task.projectKey())
                        .filter(TaskBoardProject::publicVisible)
                        .isPresent())
                .map(this::publicTask);
    }

    @Override
    public Optional<TaskBoardTaskView> findTaskView(UUID taskId) {
        if (!dataHandler.isAvailable()) {
            return Optional.empty();
        }
        Optional<TaskBoardTask> taskOptional = dataHandler.findTask(taskId);
        if (taskOptional.isEmpty()) {
            return Optional.empty();
        }
        TaskBoardTask task = taskOptional.get();
        Optional<TaskBoardProject> projectOptional = dataHandler.findProject(task.projectKey());
        if (projectOptional.isEmpty()) {
            return Optional.empty();
        }
        List<TaskBoardResolvedActionView> actions = dataHandler.findActions(taskId).stream()
                .map(this::resolvedAction)
                .toList();
        return Optional.of(new TaskBoardTaskView(
                projectOptional.get(),
                task,
                actions,
                dataHandler.findComments(taskId)
        ));
    }

    private TaskBoardProjectSummaryView summary(TaskBoardProject project) {
        List<TaskBoardTask> tasks = dataHandler.findTasks(project.projectKey());
        int activeCount = (int) tasks.stream()
                .filter(task -> task.status() == TaskBoardTaskStatus.ACTIVE
                        || task.status() == TaskBoardTaskStatus.REVIEW)
                .count();
        int blockedCount = (int) tasks.stream()
                .filter(task -> task.status() == TaskBoardTaskStatus.BLOCKED)
                .count();
        int nhvCount = (int) tasks.stream()
                .filter(task -> task.qaStatus() != TaskBoardQaStatus.HUMAN_VERIFIED)
                .count();
        int verifiedCount = (int) tasks.stream()
                .filter(TaskBoardTask::humanVerified)
                .count();
        return new TaskBoardProjectSummaryView(
                project,
                tasks.size(),
                activeCount,
                blockedCount,
                nhvCount,
                verifiedCount
        );
    }

    private TaskBoardPublicProjectSummaryView publicSummary(TaskBoardProject project) {
        return publicSummary(project, publicTasks(project.projectKey()));
    }

    private TaskBoardPublicProjectSummaryView publicSummary(
            TaskBoardProject project,
            List<TaskBoardTask> tasks
    ) {
        int activeCount = (int) tasks.stream()
                .filter(task -> task.status() == TaskBoardTaskStatus.ACTIVE
                        || task.status() == TaskBoardTaskStatus.REVIEW)
                .count();
        int completedCount = (int) tasks.stream().filter(TaskBoardTask::completed).count();
        int nhvCount = (int) tasks.stream()
                .filter(task -> task.qaStatus() != TaskBoardQaStatus.HUMAN_VERIFIED)
                .count();
        int sectionCount = (int) tasks.stream().map(TaskBoardTask::sectionKey).distinct().count();
        return new TaskBoardPublicProjectSummaryView(
                project.projectKey(),
                project.displayName(),
                project.description(),
                project.status(),
                weightedPercent(tasks),
                tasks.size(),
                activeCount,
                completedCount,
                nhvCount,
                sectionCount
        );
    }

    private List<TaskBoardTask> publicTasks(String projectKey) {
        return dataHandler.findTasks(projectKey).stream()
                .filter(TaskBoardTask::publicVisible)
                .sorted(Comparator.comparingInt(TaskBoardTask::sortOrder)
                        .thenComparing(TaskBoardTask::title))
                .toList();
    }

    private List<TaskBoardPublicSectionView> publicSections(List<TaskBoardTask> tasks) {
        Map<String, List<TaskBoardTask>> tasksBySection = new LinkedHashMap<>();
        for (TaskBoardTask task : tasks) {
            tasksBySection.computeIfAbsent(task.sectionKey(), ignored -> new java.util.ArrayList<>())
                    .add(task);
        }
        return tasksBySection.entrySet().stream()
                .map(entry -> publicSection(entry.getKey(), entry.getValue()))
                .toList();
    }

    private TaskBoardPublicSectionView publicSection(
            String sectionKey,
            List<TaskBoardTask> tasks
    ) {
        int totalWeight = tasks.stream().mapToInt(TaskBoardTask::weight).sum();
        int completedWeight = tasks.stream()
                .filter(TaskBoardTask::completed)
                .mapToInt(TaskBoardTask::weight)
                .sum();
        int activeCount = (int) tasks.stream()
                .filter(task -> task.status() == TaskBoardTaskStatus.ACTIVE
                        || task.status() == TaskBoardTaskStatus.REVIEW)
                .count();
        int completedCount = (int) tasks.stream().filter(TaskBoardTask::completed).count();
        int nhvCount = (int) tasks.stream()
                .filter(task -> task.qaStatus() != TaskBoardQaStatus.HUMAN_VERIFIED)
                .count();
        return new TaskBoardPublicSectionView(
                sectionKey,
                percent(completedWeight, totalWeight),
                completedWeight,
                totalWeight,
                tasks.size(),
                activeCount,
                completedCount,
                nhvCount,
                tasks.stream().map(this::publicTask).toList()
        );
    }

    private List<TaskBoardPublicTaskView> publicHighlights(List<TaskBoardTask> tasks) {
        return tasks.stream()
                .sorted(Comparator.comparingInt(this::publicHighlightRank)
                        .thenComparing(TaskBoardTask::updatedAt, Comparator.reverseOrder()))
                .limit(3)
                .map(this::publicTask)
                .toList();
    }

    private int publicHighlightRank(TaskBoardTask task) {
        return switch (task.status()) {
            case ACTIVE, REVIEW -> 0;
            case READY, BACKLOG -> 1;
            case BLOCKED -> 2;
            case DONE -> 3;
            case CANCELLED -> 4;
        };
    }

    private TaskBoardPublicTaskView publicTask(TaskBoardTask task) {
        return new TaskBoardPublicTaskView(
                task.taskId(),
                task.projectKey(),
                task.sectionKey(),
                task.publicDisplayTitle(),
                task.publicDisplaySummary(),
                task.status(),
                task.priority(),
                task.qaStatus(),
                task.weight(),
                task.sourceType(),
                task.updatedAt()
        );
    }

    private TaskBoardPublicActivityView publicActivity(PublicTaskEntry entry) {
        TaskBoardTask task = entry.task();
        return new TaskBoardPublicActivityView(
                task.taskId(),
                task.projectKey(),
                entry.project().displayName(),
                task.sectionKey(),
                task.publicDisplayTitle(),
                task.publicDisplaySummary(),
                task.status(),
                task.qaStatus(),
                task.updatedAt()
        );
    }

    private int weightedPercent(List<TaskBoardTask> tasks) {
        int totalWeight = tasks.stream().mapToInt(TaskBoardTask::weight).sum();
        int completedWeight = tasks.stream()
                .filter(TaskBoardTask::completed)
                .mapToInt(TaskBoardTask::weight)
                .sum();
        return percent(completedWeight, totalWeight);
    }

    private int percent(int completedWeight, int totalWeight) {
        if (totalWeight <= 0) {
            return 0;
        }
        return (int) Math.round((completedWeight * 100.0D) / totalWeight);
    }

    private TaskBoardResolvedActionView resolvedAction(TaskBoardAction action) {
        return new TaskBoardResolvedActionView(
                action,
                actionTargetResolver.resolveHref(action.type(), action.target())
        );
    }

    private record PublicTaskEntry(TaskBoardProject project, TaskBoardTask task) {
    }
}
