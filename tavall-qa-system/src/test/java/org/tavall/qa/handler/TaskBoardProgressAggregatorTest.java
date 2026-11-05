package org.tavall.qa.handler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskBoardProgressAggregatorTest {
    @TempDir Path tempDir;
    private TaskBoardTestComposition composition;

    @BeforeEach
    void setUp() {
        composition = TaskBoardTestComposition.open(tempDir, Clock.systemUTC());
    }

    @AfterEach
    void tearDown() {
        composition.close();
    }

    @Test
    void calculatesWeightedProgressAndExcludesCancelledTasks() {
        TaskBoardProgressAggregator aggregator = composition.bean(TaskBoardProgressAggregator.class);

        assertEquals(50, aggregator.calculateWeightedPercent(List.of(
                TaskBoardTestTasks.task(TaskBoardTaskStatus.DONE, 3),
                TaskBoardTestTasks.task(TaskBoardTaskStatus.ACTIVE, 3),
                TaskBoardTestTasks.task(TaskBoardTaskStatus.CANCELLED, 20))));
        assertEquals(0, aggregator.calculateSectionProgress(List.of()));
    }
}
