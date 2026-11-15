package org.tavall.qa.web.routes;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Direct house-style test for {@link QaRoutes}: the compile-time route namespace and its
 * keyed path builders. No collaborators are faked (pure routing constants/utilities).
 */
final class QaRoutesTest {

    @Test
    void surfaceRoutesAreStable() {
        assertEquals("/", QaRoutes.ROOT);
        assertEquals("/projects/{projectKey}", QaRoutes.PROJECT);
        assertEquals("/projects/{projectKey}/tasks/{taskId}", QaRoutes.TASK);
        assertEquals("/workspace/{projectKey}", QaRoutes.WORKSPACE);
        assertEquals("/messages/{key}", QaRoutes.MESSAGES);
    }

    @Test
    void projectBuildsKeyedPath() {
        assertEquals("/projects/tavall-pvp", QaRoutes.project("tavall-pvp"));
        assertEquals("/projects/tavall-pvp", QaRoutes.project("  tavall-pvp  "));
    }

    @Test
    void taskBuildsKeyedPath() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        assertEquals("/projects/tavall-pvp/tasks/" + id, QaRoutes.task("tavall-pvp", id));
    }

    @Test
    void workspaceAndLegacyPaths() {
        assertEquals("/workspace/tavall-pvp", QaRoutes.workspace("tavall-pvp"));
        assertEquals("/task-board/tavall-pvp", QaRoutes.legacyProject("tavall-pvp"));
        assertEquals("/task-board/tavall-pvp/tasks/" + UUID.fromString("22222222-2222-2222-2222-222222222222"),
                QaRoutes.legacyTask("tavall-pvp", UUID.fromString("22222222-2222-2222-2222-222222222222")));
    }

    @Test
    void nullKeyRejected() {
        assertThrows(NullPointerException.class, () -> QaRoutes.project(null));
        assertThrows(NullPointerException.class, () -> QaRoutes.legacyTask(null, UUID.randomUUID()));
    }
}
