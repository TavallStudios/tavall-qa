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

    @Test
    void unavailableDatabaseRegistersFailClosedHandlersForMutationAndExternalReferences() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-26T00:00:00Z"), ZoneOffset.UTC);
        try (var composition = TaskBoardTestComposition.openWithoutTaskDatabase(
                tempDir.resolve("no-db"), clock)) {
            var mutationHandler = composition.bean(org.tavall.qa.handler.ITaskBoardMutationHandler.class);
            var externalRefHandler = composition.bean(org.tavall.qa.handler.ITaskBoardExternalReferenceHandler.class);
            var actor = TaskBoardTestSubjects.active(UUID.randomUUID(), UUID.randomUUID(),
                    Set.of(), Optional.empty(), Set.of(QaEnvironmentGrant.DEV),
                    Set.of(org.tavall.qa.domain.auth.QaCapability.MANAGE_PROJECTS), List.of(), true, true);
            assertThrows(org.tavall.qa.domain.TaskBoardStorageUnavailableException.class,
                    () -> mutationHandler.createProject(actor,
                            new org.tavall.qa.request.TaskBoardProjectCreateRequest(
                                    "qa-unavailable", "QA Unavailable", "desc",
                                    org.tavall.qa.domain.TaskBoardProjectStatus.ACTIVE,
                                    org.tavall.qa.domain.TaskBoardAudience.PUBLIC, 1)));
            assertThrows(org.tavall.qa.domain.TaskBoardStorageUnavailableException.class,
                    () -> externalRefHandler.findOpenConflicts(actor, Optional.empty()));
        }
    }
}
