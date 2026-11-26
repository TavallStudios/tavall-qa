package org.tavall.qa.web.pages.venture;

import org.tavall.qa.domain.TaskBoardProjectKey;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardVenture;
import org.tavall.qa.view.TaskBoardPublicProjectSummaryView;
import org.tavall.qa.view.TaskBoardPublicProjectView;
import org.tavall.qa.view.TaskBoardPublicTaskView;
import org.tavall.web.frontend.design.ForgeLink;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * One venture as the QA board presents it: purpose, stage, live links, recorded metrics and open work.
 * Fields the owner has not recorded yet stay empty and are shown as "not recorded", never invented.
 */
public record QaVentureView(
        String key,
        String name,
        String purpose,
        List<String> ownerRepos,
        String topBlocker,
        String stage,
        List<ForgeLink> liveLinks,
        List<Metric> metrics,
        int progressPercent,
        int taskCount,
        List<OpenTask> openTasks
) {
    private static final Set<TaskBoardTaskStatus> CLOSED = Set.of(TaskBoardTaskStatus.DONE, TaskBoardTaskStatus.CANCELLED);

    public QaVentureView {
        key = Objects.requireNonNull(key, "key").trim();
        name = Objects.requireNonNull(name, "name").trim();
        purpose = Objects.requireNonNullElse(purpose, "").trim();
        ownerRepos = List.copyOf(ownerRepos);
        topBlocker = Objects.requireNonNullElse(topBlocker, "").trim();
        stage = Objects.requireNonNullElse(stage, "").trim();
        liveLinks = List.copyOf(liveLinks);
        metrics = List.copyOf(metrics);
        openTasks = List.copyOf(openTasks);
    }

    /** A recorded measurement and where it came from. */
    public record Metric(String label, String value, String source) {
        public Metric {
            label = Objects.requireNonNull(label, "label");
            value = Objects.requireNonNull(value, "value");
            source = Objects.requireNonNullElse(source, "");
        }
    }

    public record OpenTask(String title, TaskBoardTaskStatus status, String href) {
        public OpenTask {
            title = Objects.requireNonNull(title, "title");
            status = Objects.requireNonNull(status, "status");
            href = Objects.requireNonNull(href, "href");
        }
    }

    public int openTaskCount() {
        return openTasks.size();
    }

    public QaVentureView(String key, String name, String purpose, String stage,
                         List<ForgeLink> liveLinks, List<Metric> metrics, int progressPercent,
                         int taskCount, List<OpenTask> openTasks) {
        this(key, name, purpose, List.of(), "", stage, liveLinks, metrics,
                progressPercent, taskCount, openTasks);
    }

    public static QaVentureView fromVenture(TaskBoardVenture venture) {
        return fromVenture(venture, List.of());
    }

    /** Venture tasks are filtered by owner repository and retain the main engineering project identity. */
    public static QaVentureView fromVenture(TaskBoardVenture venture, List<TaskBoardTask> linkedTasks) {
        List<ForgeLink> links = venture.liveUrls().stream()
                .map(url -> new ForgeLink(url, url))
                .toList();
        String name = java.util.Arrays.stream(venture.slug().split("-"))
                .map(part -> part.isEmpty() ? part : Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
        List<OpenTask> open = linkedTasks.stream()
                .filter(task -> !CLOSED.contains(task.status()))
                .sorted(Comparator.comparing(TaskBoardTask::updatedAt).reversed())
                .map(task -> new OpenTask(task.title(), task.status(),
                        org.tavall.qa.web.routes.QaRoutes.qaTask(TaskBoardProjectKey.ENGINEERING.value(), task.taskId())))
                .toList();
        return new QaVentureView(venture.slug(), name, venture.purpose(), venture.ownerRepos(),
                venture.topBlocker(), venture.stage(), links, List.of(), -1, linkedTasks.size(), open);
    }

    /**
     * A board project as a venture: its description is the purpose, its board status the stage, and its
     * not-done tasks the open work. Live links and metrics come only from recorded venture data.
     */
    public static QaVentureView fromProject(TaskBoardPublicProjectView project, List<ForgeLink> liveLinks,
                                            List<Metric> metrics, Function<TaskBoardPublicTaskView, String> taskHref) {
        TaskBoardPublicProjectSummaryView summary = Objects.requireNonNull(project, "project").summary();
        List<OpenTask> open = project.sections().stream()
                .flatMap(section -> section.tasks().stream())
                .filter(task -> !CLOSED.contains(task.status()))
                .sorted(Comparator.comparing(TaskBoardPublicTaskView::priority).reversed()
                        .thenComparing(TaskBoardPublicTaskView::title))
                .map(task -> new OpenTask(task.title(), task.status(), taskHref.apply(task)))
                .toList();
        return new QaVentureView(summary.projectKey(), summary.displayName(), summary.description(),
                List.of(), "", summary.status().name(), liveLinks, metrics,
                summary.progressPercent(), summary.taskCount(), open);
    }
}
