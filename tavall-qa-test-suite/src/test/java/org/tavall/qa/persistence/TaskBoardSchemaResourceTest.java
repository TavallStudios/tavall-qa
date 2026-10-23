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
    void canonicalMigrationOwnsTaskGraphAndWeightedProgressAuthority() throws IOException {
        String migration = resource("schema/postgres/027_task_board.sql");

        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS task_board_project"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS task_board_task"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS task_board_action"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS task_board_comment"));
        assertTrue(migration.contains("section_key VARCHAR(128)"));
        assertTrue(migration.contains("weight INTEGER NOT NULL DEFAULT 1"));
        assertTrue(migration.contains("source_reference VARCHAR(1024)"));
        assertTrue(migration.contains("'PUBLIC', 'QA_TESTER', 'STAFF', 'ADMIN_ONLY'"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS qa_vqc_contribution"));
        assertFalse(migration.contains("public_progress_task_id"));
        assertFalse(migration.contains("ALTER TABLE progress_task"));
    }

    @Test
    void qaWorkflowMigrationOwnsEvidenceAssignmentsAndAppendOnlyAudit() throws IOException {
        String migration = resource("schema/postgres/028_task_board_qa_workflows.sql");

        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS task_board_evidence"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS task_board_assignment"));
        assertTrue(migration.contains("CREATE TABLE IF NOT EXISTS task_board_audit"));
        assertTrue(migration.contains("LEGACY_VERIFICATION_SUPERSEDED"));
        assertTrue(migration.contains("WHERE qa_status = 'HUMAN_VERIFIED'"));
        assertTrue(migration.contains("public_summary_approved"));
        assertTrue(migration.contains("verified_evidence_id IS NOT NULL"));
        assertTrue(migration.contains("uq_qa_vqc_contribution_task_account"));
        assertTrue(migration.contains("task_board_audit_no_row_mutation"));
        assertTrue(migration.contains("task_board_audit_no_truncate"));
    }

    private String resource(String path) throws IOException {
        ClassLoader classLoader = TaskBoardSchemaResourceTest.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(path)) {
            assertNotNull(inputStream, "Missing resource: " + path);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
