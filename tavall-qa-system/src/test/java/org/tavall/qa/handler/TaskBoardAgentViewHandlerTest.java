package org.tavall.qa.handler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.qa.domain.auth.QaSubjectContext;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TaskBoardAgentViewHandlerTest {
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
    void anonymousSubjectGetsNoTaskViewsOrContext() {
        TaskBoardAgentViewHandler handler = composition.bean(TaskBoardAgentViewHandler.class);
        QaSubjectContext anonymous = QaSubjectContext.anonymous();

        assertTrue(handler.findAuthorizedAgentTasks(anonymous, Optional.empty()).isEmpty());
        assertTrue(handler.findAgentTaskContext(anonymous, java.util.UUID.randomUUID()).isEmpty());
    }
}
