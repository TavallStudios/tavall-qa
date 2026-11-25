package org.tavall.qa.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TaskBoardVentureTest {
    @Test
    void ventureKeepsTypedIdentityAndDefensivelyCopiesRepositoryLists() {
        List<String> ownerRepos = new ArrayList<>(List.of(" tavall-qa "));

        TaskBoardVenture venture = new TaskBoardVenture(
                " tavall-qa ", "venture/tavall-qa/venture-card", " QA authority ", ownerRepos,
                "LIVE", List.of(), " Fields modeled ", null);
        ownerRepos.add("unexpected");

        assertEquals("tavall-qa", venture.slug());
        assertEquals("QA authority", venture.purpose());
        assertEquals(List.of("tavall-qa"), venture.ownerRepos());
        assertEquals("venture/tavall-qa/venture-card", venture.sourceTaskKey());
        assertThrows(UnsupportedOperationException.class, () -> venture.ownerRepos().add("mutate"));
    }

    @Test
    void ventureRejectsMissingIdentityAndBlankRepositoryNames() {
        assertThrows(IllegalArgumentException.class, () -> new TaskBoardVenture(
                " ", "venture/x/venture-card", "purpose", List.of(), "LIVE", List.of(), "blocker", null));
        assertThrows(IllegalArgumentException.class, () -> new TaskBoardVenture(
                "x", "venture/x/venture-card", "purpose", List.of(" "), "LIVE", List.of(), "blocker", null));
    }
}
