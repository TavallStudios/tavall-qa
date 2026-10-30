package org.tavall.qa.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.filesystem.api.FilesystemAtomicOperations;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.notion.NotionTaskSyncState;
import org.tavall.qa.persistence.entity.NotionTaskSyncStateEntity;
import org.tavall.qa.persistence.entity.TaskBoardAuditEntryEntity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Persistence adapter for Notion synchronization state.
 * PostgreSQL is authoritative when available; file-backed redundancy on NFS via tavall-filesystem
 * guarantees restart-surviving state and disaster recovery.
 */
@DelegatesTo(ITaskBoardNotionSyncDataHandler.class)
public final class TaskBoardNotionSyncDataHandler implements ITaskBoardNotionSyncDataHandler {
    private static final String DEFAULT_MIRROR_DIR = "/srv/dev-storage/qa-file-mirror";
    private static final String NOTION_SYNC_SUBDIR = "qa/notion-sync";

    private final IPostgresDatabase database;
    private final FilesystemAtomicOperations atomicOps;
    private final ObjectMapper objectMapper;
    private final Path mirrorRoot;

    // In-memory cache for fast lookup and fallback when DB is unavailable
    private final ConcurrentHashMap<String, NotionTaskSyncState> pageIdIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> taskToPageId = new ConcurrentHashMap<>();

    public TaskBoardNotionSyncDataHandler(
            IPostgresDatabase database,
            FilesystemAtomicOperations atomicOps,
            ObjectMapper objectMapper,
            Path mirrorRoot
    ) {
        this.database = database;
        this.atomicOps = atomicOps;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.mirrorRoot = mirrorRoot != null ? mirrorRoot : Path.of(DEFAULT_MIRROR_DIR);
        loadMirrorRecords();
    }

    public TaskBoardNotionSyncDataHandler(
            IPostgresDatabase database,
            FilesystemAtomicOperations atomicOps,
            ObjectMapper objectMapper
    ) {
        this(database, atomicOps, objectMapper, Path.of(DEFAULT_MIRROR_DIR));
    }

    @Override
    public boolean isAvailable() {
        return (database != null && database.isAvailable() && database.entities().isOpen())
                || atomicOps != null;
    }

    @Override
    public Optional<NotionTaskSyncState> findSyncStateByPageId(String pageId) {
        Objects.requireNonNull(pageId, "pageId");
        if (database != null && database.isAvailable() && database.entities().isOpen()) {
            List<NotionTaskSyncStateEntity> found = database.entities().findNamed(
                    NotionTaskSyncStateEntity.class,
                    NotionTaskSyncStateEntity.FIND_BY_PAGE_ID,
                    Map.of("notionPageId", pageId),
                    1
            );
            if (!found.isEmpty()) {
                NotionTaskSyncState state = found.get(0).toDomain();
                cache(state);
                return Optional.of(state);
            }
        }
        return Optional.ofNullable(pageIdIndex.get(pageId));
    }

    @Override
    public Optional<NotionTaskSyncState> findSyncStateByTaskId(UUID taskId) {
        Objects.requireNonNull(taskId, "taskId");
        if (database != null && database.isAvailable() && database.entities().isOpen()) {
            List<NotionTaskSyncStateEntity> found = database.entities().findNamed(
                    NotionTaskSyncStateEntity.class,
                    NotionTaskSyncStateEntity.FIND_BY_TASK_ID,
                    Map.of("taskId", taskId.toString()),
                    1
            );
            if (!found.isEmpty()) {
                NotionTaskSyncState state = found.get(0).toDomain();
                cache(state);
                return Optional.of(state);
            }
        }
        String pageId = taskToPageId.get(taskId);
        return pageId != null ? Optional.ofNullable(pageIdIndex.get(pageId)) : Optional.empty();
    }

    @Override
    public List<NotionTaskSyncState> findSyncStatesByDatabaseId(String databaseId) {
        Objects.requireNonNull(databaseId, "databaseId");
        if (database != null && database.isAvailable() && database.entities().isOpen()) {
            return database.entities().findNamed(
                    NotionTaskSyncStateEntity.class,
                    NotionTaskSyncStateEntity.FIND_BY_DATABASE_ID,
                    Map.of("notionDatabaseId", databaseId),
                    0
            ).stream().map(NotionTaskSyncStateEntity::toDomain).toList();
        }
        return pageIdIndex.values().stream()
                .filter(state -> databaseId.equals(state.notionDatabaseId()))
                .toList();
    }

    @Override
    public List<NotionTaskSyncState> findPendingOutboundSyncStates(Optional<String> databaseId) {
        Objects.requireNonNull(databaseId, "databaseId");
        if (database != null && database.isAvailable() && database.entities().isOpen()) {
            Stream<NotionTaskSyncState> stream = database.entities().findNamed(
                    NotionTaskSyncStateEntity.class,
                    NotionTaskSyncStateEntity.FIND_PENDING_OUTBOUND,
                    Map.of(),
                    0
            ).stream().map(NotionTaskSyncStateEntity::toDomain);
            if (databaseId.isPresent()) {
                stream = stream.filter(s -> databaseId.get().equals(s.notionDatabaseId()));
            }
            return stream.toList();
        }
        return pageIdIndex.values().stream()
                .filter(NotionTaskSyncState::hasPendingOutboundChanges)
                .filter(state -> databaseId.isEmpty() || databaseId.get().equals(state.notionDatabaseId()))
                .toList();
    }

    @Override
    public NotionTaskSyncState saveSyncState(NotionTaskSyncState syncState, TaskBoardAuditEntry auditEntry) {
        Objects.requireNonNull(syncState, "syncState");
        cache(syncState);

        // 1. PostgreSQL authority
        if (database != null && database.isAvailable() && database.entities().isOpen()) {
            database.entities().executeAtomic(context -> {
                context.save(NotionTaskSyncStateEntity.fromDomain(syncState));
                if (auditEntry != null) {
                    context.save(TaskBoardAuditEntryEntity.fromDomain(auditEntry));
                }
                return null;
            });
        }

        // 2. File-backed redundancy mirror on NFS
        persistToFileMirror(syncState);

        return syncState;
    }

    private void cache(NotionTaskSyncState state) {
        pageIdIndex.put(state.notionPageId(), state);
        taskToPageId.put(state.taskId(), state.notionPageId());
    }

    private void persistToFileMirror(NotionTaskSyncState state) {
        if (atomicOps == null) {
            return;
        }
        try {
            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(state);
            Path directory = atomicOps.createContainedDirectories(mirrorRoot, NOTION_SYNC_SUBDIR);
            String safeFileName = sanitizeFileName(state.notionPageId()) + ".json";
            Path target = atomicOps.resolveContained(mirrorRoot, NOTION_SYNC_SUBDIR + "/" + safeFileName);
            atomicOps.replaceAtomically(target, bytes);
        } catch (Exception exception) {
            // File redundancy failure must not silently crash main flow, but log
            System.getLogger(TaskBoardNotionSyncDataHandler.class.getName())
                    .log(System.Logger.Level.WARNING, "Failed to mirror Notion sync state to file: " + exception.getMessage());
        }
    }

    private void loadMirrorRecords() {
        if (atomicOps == null) {
            return;
        }
        try {
            Path syncDir = mirrorRoot.resolve(NOTION_SYNC_SUBDIR);
            if (!Files.isDirectory(syncDir)) {
                return;
            }
            try (Stream<Path> stream = Files.list(syncDir)) {
                stream.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                    try {
                        byte[] bytes = Files.readAllBytes(path);
                        NotionTaskSyncState state = objectMapper.readValue(bytes, NotionTaskSyncState.class);
                        cache(state);
                    } catch (Exception ignored) {
                    }
                });
            }
        } catch (Exception ignored) {
        }
    }

    private static String sanitizeFileName(String input) {
        return input.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
