package org.tavall.qa.domain.notion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class NotionTaskMappingRegistryTest {
    @Test
    void canonicalRegistryProvidesMappingsForBothSupportedTaskDatabases() {
        NotionTaskMappingRegistry registry = NotionTaskMappingRegistry.canonical();

        assertEquals("tavall-pvp", registry.findMapping("3a338458-ddfd-80eb-9bbf-cb8bc741e610")
                .orElseThrow().defaultProjectKey());
        assertEquals("tavall-kingdoms", registry.findMapping("B3F34D39223B43378A60531CD7E6FDBF")
                .orElseThrow().defaultProjectKey());
    }

    @Test
    void registeringAndReplacingAMappingUsesNormalizedDatabaseIdentity() {
        NotionTaskMappingRegistry registry = emptyRegistry();
        NotionDatabaseTaskMapping first = mapping("qa-db-1", "Original", "project-a");
        NotionTaskMappingConfiguration beforeRegistration = registry.configuration();

        registry.registerMapping(first);
        NotionTaskMappingConfiguration afterFirstRegistration = registry.configuration();
        NotionDatabaseTaskMapping replacement = mapping("QADB1", "Replacement", "project-b");
        registry.registerMapping(replacement);

        assertEquals(Map.of(), beforeRegistration.mappings());
        assertSame(first, afterFirstRegistration.findMapping("QA-DB-1").orElseThrow());
        assertSame(replacement, registry.findMapping("qa-db-1").orElseThrow());
        assertEquals("project-b", registry.findMapping("QADB1").orElseThrow().defaultProjectKey());
    }

    @Test
    void rejectsANullMapping() {
        NotionTaskMappingRegistry registry = emptyRegistry();

        assertThrows(NullPointerException.class, () -> registry.registerMapping(null));
    }

    private static NotionTaskMappingRegistry emptyRegistry() {
        return new NotionTaskMappingRegistry(new NotionTaskMappingConfiguration(
                NotionTaskMappingConfiguration.CURRENT_VERSION, Map.of()));
    }

    private static NotionDatabaseTaskMapping mapping(String databaseId, String databaseName, String projectKey) {
        NotionDatabaseTaskMapping defaults = NotionDatabaseTaskMapping.canonicalPvpTasks();
        return new NotionDatabaseTaskMapping(
                databaseId,
                databaseName,
                projectKey,
                defaults.titleProperty(),
                defaults.statusProperty(),
                defaults.statusMapping(),
                defaults.outboundStatusMapping(),
                defaults.ownerProperty(),
                defaults.dueDateProperty(),
                defaults.currentStepProperty(),
                defaults.checklistProgressProperty(),
                defaults.blockedProperty(),
                defaults.blockerSummaryProperty(),
                defaults.evidenceCompleteProperty(),
                defaults.projectProperty(),
                defaults.domainProperty(),
                defaults.domainTagsProperty(),
                defaults.linearIdProperty(),
                defaults.linearParentUrlProperty(),
                defaults.lastReconciledProperty()
        );
    }
}
