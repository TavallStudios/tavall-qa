package org.tavall.qa.handler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.qa.domain.TaskBoardActionType;

import java.nio.file.Path;
import java.time.Clock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TaskBoardActionTargetResolverTest {
    @TempDir Path tempDir;
    private TaskBoardTestComposition composition;
    private TaskBoardActionTargetResolver resolver;

    @BeforeEach
    void setUp() {
        composition = TaskBoardTestComposition.open(tempDir, Clock.systemUTC());
        resolver = composition.bean(TaskBoardActionTargetResolver.class);
    }

    @AfterEach
    void tearDown() {
        composition.close();
    }

    @Test
    void resolvesAdminRouteWithoutLeavingAdminAuthority() {
        assertEquals(
                "/admin/messages",
                resolver.resolveHref(TaskBoardActionType.ADMIN_ROUTE, "/admin/messages")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolveHref(TaskBoardActionType.ADMIN_ROUTE, "/store")
        );
    }

    @Test
    void resolvesRepositoryFileFromTypedTarget() {
        assertEquals(
                "https://github.com/TavallStudios/tavall-project-novus/blob/main/novus-web/README.md",
                resolver.resolveHref(
                        TaskBoardActionType.REPOSITORY_FILE,
                        "TavallStudios/tavall-project-novus@main:novus-web/README.md"
                )
        );
    }

    @Test
    void rejectsRepositoryTraversal() {
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolveHref(
                        TaskBoardActionType.REPOSITORY_FILE,
                        "TavallStudios/tavall-project-novus@main:../secret"
                )
        );
    }

    @Test
    void externalTargetsRequireHttpsAndDiscordTargetsStayDiscord() {
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolveHref(TaskBoardActionType.EXTERNAL_LINK, "http://example.com")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> resolver.resolveHref(TaskBoardActionType.DISCORD_THREAD, "https://example.com/thread")
        );
        assertEquals(
                "https://discord.com/channels/1/2/3",
                resolver.resolveHref(
                        TaskBoardActionType.DISCORD_THREAD,
                        "https://discord.com/channels/1/2/3"
                )
        );
    }
}
