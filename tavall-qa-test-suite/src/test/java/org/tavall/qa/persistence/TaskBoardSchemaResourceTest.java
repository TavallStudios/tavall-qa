package org.tavall.qa.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TaskBoardSchemaResourceTest {
    @Test
    void migrationOwnsPublicTaskGraphAndHumanVerificationConstraint() throws IOException {
        String migration = resource("schema/postgres/027_task_board.sql");

        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS task_board_project"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS task_board_task"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS task_board_action"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS task_board_comment"));
        assertTrue(migration.contains("section_key VARCHAR(128)"));
        assertTrue(migration.contains("weight INTEGER NOT NULL DEFAULT 1"));
        assertTrue(migration.contains("source_reference VARCHAR(1024)"));
        assertTrue(migration.contains("'PUBLIC', 'QA_TESTER', 'STAFF', 'ADMIN_ONLY'"));
        assertTrue(migration.contains("qa_status = 'HUMAN_VERIFIED'"));
        assertTrue(migration.contains("verified_by_minecraft_uuid IS NOT NULL"));
        assertTrue(migration.contains("human_verified_at IS NOT NULL"));
        assertFalse(migration.contains("public_progress_task_id"));
        assertFalse(migration.contains("ALTER TABLE progress_task"));
    }

    private String resource(String path) throws IOException {
        ClassLoader classLoader = TaskBoardSchemaResourceTest.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(path)) {
            assertNotNull(inputStream, "Missing resource: " + path);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
