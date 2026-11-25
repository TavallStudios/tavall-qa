package org.tavall.qa.handler;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardAuditEventType;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardProjectKey;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.TaskBoardVenture;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.auth.QaSubjectStatus;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.persistence.ITaskBoardVentureDataHandler;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TaskBoardVentureQueryHandlerTest {
    @Test
    void ventureTaskFilterUsesEngineeringBoardAndOwnerRepository() {
        TaskBoardVenture venture = new TaskBoardVenture(
                "tavall-qa", "venture/tavall-qa/venture-card", "QA authority",
                List.of("tavall-qa"), "LIVE", List.of(), "No blocker", null);
        InMemoryTaskBoardDataHandler tasks = new InMemoryTaskBoardDataHandler();
        TaskBoardTask linkedTask = task("tavall-engineering",
                "https://github.com/TavallStudios/tavall-qa/issues/38", 1);
        TaskBoardTask unrelatedRepository = task("tavall-engineering",
                "https://github.com/TavallStudios/tavall-web/issues/22", 2);
        TaskBoardTask otherProject = task("project-novus",
                "https://github.com/TavallStudios/tavall-qa/issues/99", 3);
        save(tasks, linkedTask);
        save(tasks, unrelatedRepository);
        save(tasks, otherProject);
        ITaskBoardVentureDataHandler ventures = new InMemoryVentureDataHandler(List.of(venture));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ITaskBoardDataHandler.class, () -> tasks);
            context.registerBean(ITaskBoardVentureDataHandler.class, () -> ventures);
            context.registerBean(TaskBoardVentureQueryHandler.class);
            context.refresh();

            TaskBoardVentureQueryHandler subject = context.getBean(TaskBoardVentureQueryHandler.class);
            QaSubjectContext manager = ventureManager();

            assertEquals(List.of(venture), subject.findVentures(manager));
            assertEquals(List.of(linkedTask), subject.findVentureTasks("tavall-qa", manager));
            assertThrows(SecurityException.class,
                    () -> subject.findVentures(QaSubjectContext.anonymous()));
        }
    }

    @Test
    void ventureTaskFilterMatchesTypedRepositoryIdentityAcrossSourceForms() {
        TaskBoardVenture venture = new TaskBoardVenture(
                "tavall-qa", "venture/tavall-qa/venture-card", "QA authority",
                List.of("TavallStudios/tavall-qa"), "LIVE", List.of(), "No blocker", null);
        InMemoryTaskBoardDataHandler tasks = new InMemoryTaskBoardDataHandler();
        TaskBoardTask issueUrl = task(TaskBoardProjectKey.ENGINEERING.value(),
                "https://github.com/TavallStudios/tavall-qa/issues/38", 1);
        TaskBoardTask providerPath = task(TaskBoardProjectKey.ENGINEERING.value(),
                "github/TavallStudios/tavall-qa/issues/39", 2);
        TaskBoardTask ventureCard = task(TaskBoardProjectKey.ENGINEERING.value(),
                "venture/tavall-qa/venture-card", 3);
        TaskBoardTask similarRepository = task(TaskBoardProjectKey.ENGINEERING.value(),
                "https://github.com/TavallStudios/tavall-web-qa/issues/5", 4);
        TaskBoardTask otherProject = task("project-novus",
                "https://github.com/TavallStudios/tavall-qa/issues/99", 5);
        save(tasks, issueUrl);
        save(tasks, providerPath);
        save(tasks, ventureCard);
        save(tasks, similarRepository);
        save(tasks, otherProject);
        ITaskBoardVentureDataHandler ventures = new InMemoryVentureDataHandler(List.of(venture));

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ITaskBoardDataHandler.class, () -> tasks);
            context.registerBean(ITaskBoardVentureDataHandler.class, () -> ventures);
            context.registerBean(TaskBoardVentureQueryHandler.class);
            context.refresh();

            TaskBoardVentureQueryHandler subject = context.getBean(TaskBoardVentureQueryHandler.class);
            List<UUID> matched = subject.findVentureTasks("tavall-qa", ventureManager()).stream()
                    .map(TaskBoardTask::taskId)
                    .sorted()
                    .toList();

            assertEquals(List.of(issueUrl.taskId(), providerPath.taskId(), ventureCard.taskId()).stream()
                    .sorted().toList(), matched);
        }
    }

    private static QaSubjectContext ventureManager() {
        return new QaSubjectContext(Optional.of(new UUID(0, 20)), QaSubjectStatus.ACTIVE,
                Optional.empty(), Optional.of(new UUID(0, 30)), true, false,
                java.util.Set.of(), Optional.empty(), java.util.Set.of(),
                java.util.Set.of(QaCapability.MANAGE_PROJECTS), List.of(), false);
    }

    private static TaskBoardTask task(String projectKey, String sourceReference, long id) {
        Instant created = Instant.parse("2026-11-01T12:00:00Z");
        return new TaskBoardTask(new UUID(0, id), projectKey, "section-test", "Task " + id,
                "Task " + id, "Description", "Summary", TaskBoardTaskStatus.ACTIVE,
                TaskBoardTaskPriority.NORMAL, TaskBoardQaStatus.NHV, TaskBoardAudience.PUBLIC, 1,
                false, "github-issue", sourceReference, true, 1, new UUID(0, 9), null,
                null, null, null, null, 1, created, created);
    }

    private static void save(InMemoryTaskBoardDataHandler dataHandler, TaskBoardTask task) {
        TaskBoardAuditEntry audit = new TaskBoardAuditEntry(UUID.randomUUID(), Optional.of(task.taskId()),
                task.projectKey(), Optional.empty(), null, TaskBoardAuditEventType.TASK_CREATED,
                "Venture query fixture", task.createdAt());
        dataHandler.saveTask(task, Optional.empty(), audit);
    }

    private static final class InMemoryVentureDataHandler implements ITaskBoardVentureDataHandler {
        private final Map<String, TaskBoardVenture> ventures;

        private InMemoryVentureDataHandler(List<TaskBoardVenture> records) {
            ventures = records.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                    TaskBoardVenture::slug, record -> record));
        }

        @Override
        public List<TaskBoardVenture> findVentures() {
            return List.copyOf(ventures.values());
        }

        @Override
        public Optional<TaskBoardVenture> findVenture(String slug) {
            return Optional.ofNullable(ventures.get(slug));
        }
    }
}
