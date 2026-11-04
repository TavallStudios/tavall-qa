package org.tavall.qa.handler;

import org.junit.jupiter.api.Test;
import org.tavall.qa.auth.TaskBoardTestSubjects;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceKey;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceProvider;
import org.tavall.qa.persistence.UnavailableTaskBoardDataHandler;
import org.tavall.qa.persistence.UnavailableTaskBoardExternalReferenceDataHandler;

import java.time.Clock;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskBoardExternalReferenceHandlerTest {
    @Test
    void referenceQueriesRequireTaskManagementCapability() {
        var subject = TaskBoardTestSubjects.assigned(UUID.randomUUID(), UUID.randomUUID(),
                QaCapability.VIEW_QA_TASKS, UUID.randomUUID(), QaEnvironmentGrant.DEV);
        var handler = new TaskBoardExternalReferenceHandler(
                new UnavailableTaskBoardExternalReferenceDataHandler(),
                new UnavailableTaskBoardDataHandler(), Clock.systemUTC());
        var key = new TaskBoardExternalReferenceKey(TaskBoardExternalReferenceProvider.GITHUB_ISSUE, "24006", "88006");

        assertThrows(SecurityException.class, () -> handler.findReference(subject, key));
    }
}
