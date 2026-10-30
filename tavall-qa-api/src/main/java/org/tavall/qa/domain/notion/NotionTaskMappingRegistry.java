package org.tavall.qa.domain.notion;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe registry owning typed, versioned Notion database task mapping configuration.
 * Per-database mappings are configured explicitly and loaded as configuration rather than constructor side effects.
 */
public final class NotionTaskMappingRegistry implements INotionTaskMappingRegistry {
    private final AtomicReference<NotionTaskMappingConfiguration> config;

    public NotionTaskMappingRegistry(NotionTaskMappingConfiguration configuration) {
        this.config = new AtomicReference<>(Objects.requireNonNull(configuration, "configuration"));
    }

    public static NotionTaskMappingRegistry canonical() {
        return new NotionTaskMappingRegistry(NotionTaskMappingConfiguration.canonical());
    }

    @Override
    public Optional<NotionDatabaseTaskMapping> findMapping(String databaseId) {
        return config.get().findMapping(databaseId);
    }

    @Override
    public void registerMapping(NotionDatabaseTaskMapping mapping) {
        Objects.requireNonNull(mapping, "mapping");
        config.updateAndGet(current -> current.withMapping(mapping));
    }

    @Override
    public NotionTaskMappingConfiguration configuration() {
        return config.get();
    }
}
