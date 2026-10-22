package org.tavall.qa.action;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardActionType;
import org.tavall.qa.handler.TaskBoardActionTargetResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TaskBoardActionTargetResolverTest {
    private final TaskBoardActionTargetResolver resolver = new TaskBoardActionTargetResolver();

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
