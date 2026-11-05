package org.tavall.qa.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.qa.auth.TaskBoardTestSubjects;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.handler.TaskBoardTestComposition;
import org.tavall.qa.persistence.ITaskBoardDataHandler;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class TavallQaConfigurationTest {
    @TempDir Path tempDir;

    @Test
    void newCompositionUsesCurrentSpringTaskCapabilityAndRetiresPreviousCheckpointHandler() {
        DependencyMap dependencies = new DependencyMap();
        Clock clock = Clock.fixed(Instant.parse("2026-10-26T00:00:00Z"), ZoneOffset.UTC);
        dependencies.registerInstance(Clock.class, clock);
        try (var previousComposition = TaskBoardTestComposition.open(
                dependencies, tempDir.resolve("previous"), clock)) {
            var previousHandler = previousComposition.bean(org.tavall.qa.handler.ITaskBoardExecutionCheckpointHandler.class);
            assertTrue(dependencies.findInstance(ITaskBoardDataHandler.class).isAvailable());

            try (var currentComposition = TaskBoardTestComposition.open(
                    dependencies, tempDir.resolve("current"), clock)) {
                var currentHandler = currentComposition.bean(org.tavall.qa.handler.ITaskBoardExecutionCheckpointHandler.class);
                assertTrue(dependencies.findInstance(ITaskBoardDataHandler.class).isAvailable());
                assertSame(clock, dependencies.findInstance(Clock.class));
                assertNotSame(previousHandler, currentHandler);
                var actor = TaskBoardTestSubjects.active(UUID.randomUUID(), UUID.randomUUID(), Set.of(),
                        Optional.empty(), Set.of(QaEnvironmentGrant.DEV), Set.of(), List.of(), false, true);
                UUID task = UUID.randomUUID();
                assertThrows(IllegalStateException.class, () -> previousHandler.currentLease(actor, task));
                assertThrows(IllegalArgumentException.class, () -> currentHandler.currentLease(actor, task));
            }
        }
    }
}
