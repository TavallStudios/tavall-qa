package org.tavall.qa.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.qa.auth.TaskBoardTestSubjects;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardDesignCandidate;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.request.TaskBoardDesignReviewDecisionRequest;
import org.tavall.qa.request.TaskBoardDesignReviewRegisterRequest;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Real migrations, entity mapping, production composition and restart persistence. */
final class TaskBoardMutationHandlerSimulationTest {
    @TempDir Path directory;

    @Test
    void liveReviewDecisionSurvivesCompositionRestartAndCannotBeOverwritten() throws Exception {
        try (var postgres = new TaskBoardPostgresFixture(directory)) {
            UUID taskId;
            UUID actor = UUID.randomUUID();
            var owner = TaskBoardTestSubjects.manager(actor, UUID.randomUUID());
            try (var composition = new TaskBoardPostgresComposition(postgres.database(), Clock.systemUTC())) {
                var mutations = composition.bean(ITaskBoardMutationHandler.class);
                mutations.createProject(owner, new TaskBoardProjectCreateRequest("design-review", "Design review",
                        "Persisted review acceptance", TaskBoardProjectStatus.ACTIVE, TaskBoardAudience.STAFF, 0));
                var task = mutations.createTask(owner, new TaskBoardTaskCreateRequest("design-review", "dashboard",
                        "Dashboard look", "Dashboard look", "Choose a live candidate", "Choose a live candidate",
                        TaskBoardTaskPriority.HIGH, TaskBoardAudience.STAFF, 1, false, false, "test", "issue://23", 0));
                taskId = task.taskId();
                var review = mutations.registerDesignReview(owner, new TaskBoardDesignReviewRegisterRequest(taskId,
                        List.of(new TaskBoardDesignCandidate("A", "Ledger", "https://dashboard.tavall.org/__preview/design-review?direction=a", "Editorial"),
                                new TaskBoardDesignCandidate("B", "Console", "https://dashboard.tavall.org/__preview/design-review?direction=b", "Operational")), 0));
                mutations.selectDesignReviewWinner(owner, new TaskBoardDesignReviewDecisionRequest(taskId, "B", "Selected in review", review.revision()));
            }
            try (var composition = new TaskBoardPostgresComposition(postgres.database(), Clock.systemUTC())) {
                var tasks = composition.bean(ITaskBoardDataHandler.class);
                var review = tasks.findDesignReview(taskId).orElseThrow();
                assertEquals("B", review.winnerKey());
                assertEquals(actor, review.decidedByAccountId());
                assertNotNull(review.decidedAt());
                assertEquals("Selected in review", review.note());
                assertEquals(2, review.revision());
                assertEquals(TaskBoardTaskStatus.READY, tasks.findTask(taskId).orElseThrow().status());
                assertThrows(IllegalStateException.class, () -> composition.bean(ITaskBoardMutationHandler.class)
                        .selectDesignReviewWinner(owner, new TaskBoardDesignReviewDecisionRequest(taskId, "A", "", review.revision())));
                assertEquals("B", tasks.findDesignReview(taskId).orElseThrow().winnerKey());
            }
        }
    }
}
