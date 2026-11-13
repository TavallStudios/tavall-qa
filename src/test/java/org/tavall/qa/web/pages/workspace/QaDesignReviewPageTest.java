package org.tavall.qa.web.pages.workspace;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.*;
import org.tavall.qa.view.TaskBoardTaskView;
import org.tavall.web.frontend.render.HTMLRenderer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class QaDesignReviewPageTest {
    private final HTMLRenderer renderer = new HTMLRenderer();

    @Test
    void reviewerGetsLiveComparisonAndCsrfProtectedWinnerForms() {
        QaDesignReviewPage page = QaDesignReviewPage.build(view(false), context(), true, List.of(view(false)));
        assertTrue(page.animations().animations().stream().anyMatch(animation -> animation.invocation()
                .equals(org.tavall.qa.web.generated.QaBoardTS.initializeQaBoard())));
        String html = renderer.render(page).body();
        assertTrue(html.contains("data-qa-design-control=\"width\""));
        assertTrue(html.contains("value=\"390\""));
        assertTrue(html.contains("data-qa-design-control=\"state\""));
        assertTrue(html.contains("Open design reviews"));
        assertTrue(html.contains("Surface: Product dashboard"));
        assertTrue(html.contains("<iframe"));
        assertTrue(html.contains("sandbox=\"allow-scripts allow-same-origin\""));
        assertTrue(html.contains("referrerpolicy=\"no-referrer\""));
        assertTrue(html.contains("name=\"_csrf\" value=\"test-csrf\""));
        assertTrue(html.contains("Select A as winner"));
        assertTrue(html.contains("Select B as winner"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertFalse(html.contains("<script>"));
    }

    @Test
    void viewerAndDecidedReviewCannotRenderWinnerMutationControls() {
        String viewer = renderer.render(QaDesignReviewPage.build(view(false), context(), false, List.of())).body();
        String decided = renderer.render(QaDesignReviewPage.build(view(true), context(), true, List.of())).body();
        assertFalse(viewer.contains("as winner"));
        assertFalse(decided.contains("as winner"));
        assertTrue(decided.contains("Selected: B"));
        assertTrue(decided.contains("Accepted hierarchy"));
        assertTrue(viewer.contains("<iframe"));
    }

    private static TaskBoardTaskView view(boolean decided) {
        Instant now = Instant.parse("2026-11-12T00:00:00Z");
        UUID taskId = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        TaskBoardProject project = new TaskBoardProject("dashboard", "Dashboard", "Shared dashboard",
                TaskBoardProjectStatus.ACTIVE, TaskBoardAudience.STAFF, 0, now, now);
        TaskBoardTask task = new TaskBoardTask(taskId, "dashboard", "design", "Choose a dashboard look",
                "Dashboard look", "Choose a live candidate", "", TaskBoardTaskStatus.REVIEW,
                TaskBoardTaskPriority.HIGH, TaskBoardQaStatus.NHV, TaskBoardAudience.STAFF, 1, false,
                "github", "https://github.com/TavallStudios/tavall-qa/issues/23", false, actor, null,
                null, null, null, null, 0, now, now);
        TaskBoardDesignReview review = TaskBoardDesignReview.register(taskId, "Product dashboard", List.of(
                new TaskBoardDesignCandidate("A", "Ledger", "https://dashboard.tavall.org/__preview/design-review?direction=a", "Editorial <script>"),
                new TaskBoardDesignCandidate("B", "Console", "https://dashboard.tavall.org/__preview/design-review?direction=b", "Operational",
                        java.util.Map.of("state", List.of("loaded", "empty", "error")))), 1, actor, now);
        if (decided) review = review.select("B", actor, "Accepted hierarchy", now);
        return new TaskBoardTaskView(project, task, List.of(), List.of(), List.of(), List.of(), List.of(), review);
    }

    private static QaWorkspacePageContext context() {
        return new QaWorkspacePageContext("OVERVIEW", "CURRENT", "_csrf", "test-csrf", Optional.of(UUID.randomUUID()),
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), false, true, false);
    }
}
