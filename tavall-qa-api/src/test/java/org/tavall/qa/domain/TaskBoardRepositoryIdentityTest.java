package org.tavall.qa.domain;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Direct house-style test for {@link TaskBoardRepositoryIdentity} source-reference parsing. */
final class TaskBoardRepositoryIdentityTest {

    @Test
    void parsesEveryRecordedSourceReferenceForm() {
        assertEquals("tavall-qa", identity("https://github.com/TavallStudios/tavall-qa/issues/38").name());
        assertEquals("tavall-qa", identity("github/TavallStudios/tavall-qa/issues/38").name());
        assertEquals("tavall-qa", identity("TavallStudios/tavall-qa/issues/38").name());
        assertEquals("tavall-qa", identity("venture/tavall-qa/venture-card").name());
    }

    @Test
    void matchesOwnerRepositoriesByCompleteSegmentIgnoringCaseAndOwnerPrefix() {
        TaskBoardRepositoryIdentity identity = identity("https://github.com/TavallStudios/tavall-qa/issues/38");

        assertTrue(identity.matches("tavall-qa"));
        assertTrue(identity.matches(" TAVALL-QA "));
        assertTrue(identity.matches("TavallStudios/tavall-qa"));
        assertFalse(identity.matches("tavall-web-qa"));
        assertFalse(identity.matches(null));
    }

    @Test
    void blankOrIdentitylessReferencesHaveNoRepositoryIdentity() {
        assertTrue(TaskBoardRepositoryIdentity.fromSourceReference(null).isEmpty());
        assertTrue(TaskBoardRepositoryIdentity.fromSourceReference("   ").isEmpty());
        assertTrue(TaskBoardRepositoryIdentity.fromSourceReference("tavall-qa").isEmpty());
    }

    private static TaskBoardRepositoryIdentity identity(String sourceReference) {
        Optional<TaskBoardRepositoryIdentity> identity =
                TaskBoardRepositoryIdentity.fromSourceReference(sourceReference);
        assertTrue(identity.isPresent(), sourceReference);
        return identity.orElseThrow();
    }
}
