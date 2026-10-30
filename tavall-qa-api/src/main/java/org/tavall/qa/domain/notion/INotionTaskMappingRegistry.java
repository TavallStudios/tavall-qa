package org.tavall.qa.domain.notion;

import java.util.Optional;

/**
 * Registry contract owning typed, versioned Notion database task mapping configuration.
 */
public interface INotionTaskMappingRegistry {
    Optional<NotionDatabaseTaskMapping> findMapping(String databaseId);

    void registerMapping(NotionDatabaseTaskMapping mapping);

    NotionTaskMappingConfiguration configuration();
}
