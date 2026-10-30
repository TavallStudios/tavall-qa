package org.tavall.qa.domain.notion;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Typed, versioned configuration holding registered Notion database task mappings.
 */
public record NotionTaskMappingConfiguration(
        int schemaVersion,
        Map<String, NotionDatabaseTaskMapping> mappings
) {
    public static final int CURRENT_VERSION = 1;

    public NotionTaskMappingConfiguration {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be >= 1");
        }
        Objects.requireNonNull(mappings, "mappings");
        mappings = Collections.unmodifiableMap(new HashMap<>(mappings));
    }

    public static NotionTaskMappingConfiguration canonical() {
        Map<String, NotionDatabaseTaskMapping> defaults = new HashMap<>();
        NotionDatabaseTaskMapping pvp = NotionDatabaseTaskMapping.canonicalPvpTasks();
        NotionDatabaseTaskMapping kingdoms = NotionDatabaseTaskMapping.canonicalKingdomsTasks();
        defaults.put(normalizeId(pvp.databaseId()), pvp);
        defaults.put(normalizeId(kingdoms.databaseId()), kingdoms);
        return new NotionTaskMappingConfiguration(CURRENT_VERSION, defaults);
    }

    public Optional<NotionDatabaseTaskMapping> findMapping(String databaseId) {
        if (databaseId == null || databaseId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(mappings.get(normalizeId(databaseId)));
    }

    public NotionTaskMappingConfiguration withMapping(NotionDatabaseTaskMapping mapping) {
        Objects.requireNonNull(mapping, "mapping");
        Map<String, NotionDatabaseTaskMapping> updated = new HashMap<>(mappings);
        updated.put(normalizeId(mapping.databaseId()), mapping);
        return new NotionTaskMappingConfiguration(schemaVersion, updated);
    }

    public static String normalizeId(String id) {
        return id == null ? "" : id.replace("-", "").toLowerCase();
    }
}
