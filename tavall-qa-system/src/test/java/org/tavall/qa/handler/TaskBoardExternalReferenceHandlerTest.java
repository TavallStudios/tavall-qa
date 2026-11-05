package org.tavall.qa.handler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.qa.auth.TaskBoardTestSubjects;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceKey;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceProvider;

import java.nio.file.Path;
import java.time.Clock;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class TaskBoardExternalReferenceHandlerTest {
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
    void referenceQueriesRequireTaskManagementCapability() {
        var subject = TaskBoardTestSubjects.assigned(UUID.randomUUID(), UUID.randomUUID(),
                QaCapability.VIEW_QA_TASKS, UUID.randomUUID(), QaEnvironmentGrant.DEV);
        var handler = composition.bean(TaskBoardExternalReferenceHandler.class);
        var key = new TaskBoardExternalReferenceKey(TaskBoardExternalReferenceProvider.GITHUB_ISSUE, "24006", "88006");

        assertThrows(SecurityException.class, () -> handler.findReference(subject, key));
    }
}
