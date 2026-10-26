package org.tavall.qa.config;

import org.junit.jupiter.api.Test;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.qa.auth.TaskBoardTestSubjects;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.persistence.UnavailableTaskBoardDataHandler;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TavallQaConfigurationTest {
    @Test
    void newCompositionUsesCurrentSpringTaskCapabilityAndRetiresPreviousCheckpointHandler() {
        DependencyMap dependencies = new DependencyMap();
        try {
            Clock clock = Clock.fixed(Instant.parse("2026-10-26T00:00:00Z"), ZoneOffset.UTC);
            dependencies.registerInstance(Clock.class, clock);
            var previousTasks = new UnavailableTaskBoardDataHandler();
            var previous = new TavallQaConfiguration(dependencies);
            var previousHandler = previous.taskBoardExecutionCheckpointHandler(previousTasks,
                    previous.taskBoardExecutionCheckpointDataHandler());
            assertSame(previousTasks, dependencies.findInstance(ITaskBoardDataHandler.class));

            var currentTasks = new UnavailableTaskBoardDataHandler();
            var current = new TavallQaConfiguration(dependencies);
            var currentHandler = current.taskBoardExecutionCheckpointHandler(currentTasks,
                    current.taskBoardExecutionCheckpointDataHandler());

            assertSame(currentTasks, dependencies.findInstance(ITaskBoardDataHandler.class));
            assertSame(clock, dependencies.findInstance(Clock.class));
            assertNotSame(previousHandler, currentHandler);
            var actor = TaskBoardTestSubjects.active(UUID.randomUUID(), UUID.randomUUID(), Set.of(),
                    Optional.empty(), Set.of(QaEnvironmentGrant.DEV), Set.of(), List.of(), false, true);
            UUID task = UUID.randomUUID();
            assertThrows(IllegalStateException.class, () -> previousHandler.currentLease(actor, task));
            assertThrows(IllegalArgumentException.class, () -> currentHandler.currentLease(actor, task));
        } finally {
            dependencies.clear();
        }
    }
}
