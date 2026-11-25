package org.tavall.qa.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.tavall.qa.domain.TaskBoardVenture;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.auth.QaSubjectStatus;
import org.tavall.qa.handler.ITaskBoardVentureQueryHandler;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.persistence.ITaskBoardVentureDataHandler;
import org.tavall.qa.persistence.TaskBoardDataHandler;
import org.tavall.qa.persistence.TaskBoardVentureDataHandler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Applies the venture migration to isolated PostgreSQL and verifies task-owned data survives reassignment. */
final class VentureMigrationPostgresIntegrationTest {
    @TempDir
    Path directory;

    @Test
    void ventureRehomingPreservesTasksAndTheirExecutionRecords() throws Exception {
        List<String> migrations = new ArrayList<>(TaskBoardPostgresFixture.CORE_MIGRATIONS);
        migrations.add("040_task_board_venture_projects.sql");
        migrations.sort(String::compareTo);

        try (TaskBoardPostgresFixture postgres = new TaskBoardPostgresFixture(directory, migrations);
             Connection connection = DriverManager.getConnection(postgres.url(), "postgres", "")) {
            seedRows(connection);
            applyVentureMigration(connection);

            assertEquals(4, count(connection, "SELECT count(*) FROM task_board_task"));
            assertEquals(3, count(connection,
                    "SELECT count(*) FROM task_board_task WHERE project_key = 'tavall-engineering'"));
            assertEquals(1, count(connection,
                    "SELECT count(*) FROM task_board_task WHERE project_key = 'tavall-pvp'"));
            assertEquals(0, count(connection,
                    "SELECT count(*) FROM task_board_project WHERE project_key IN ("
                            + "'tavall-web','tavall-times-blog','tavall-account','tavall-dashboard',"
                            + "'tavall-commerce','tavall-docs','tavall-qa','tavall-content','tavall-cloud',"
                            + "'tavall-ai','tavall-ai-chat','tavall-ai-plugin','tavall-mcp','tavall-analytics',"
                            + "'tavall-workflows','tavall-contractors','tavall-couriers','tavall-builder',"
                            + "'tavall-affiliates','tavall-labs','tavall-ops','tavall-mc-speedrun',"
                            + "'tavall-webstore','tavall-hytale','tavall-roblox','tavall-open-harness')"));
            assertEquals(3, count(connection,
                    "SELECT count(*) FROM task_board_project WHERE project_key IN "
                            + "('tavall-engineering','project-novus','tavall-pvp')"));
            assertEquals(28, count(connection, "SELECT count(*) FROM task_board_venture"));
            assertTask(connection, "40000000-0000-0000-0000-000000000001",
                    "venture/tavall-web/venture-card", "BLOCKED", "Venture card stays intact");
            assertTask(connection, "40000000-0000-0000-0000-000000000002",
                    "github/TavallStudios/tavall-web/issues/106", "ACTIVE", "Issue task stays intact");
            assertTask(connection, "40000000-0000-0000-0000-000000000008",
                    "github/TavallStudios/tavall-mc/issues/401", "READY", "Re-homed game issue returns");
            assertTaskInProject(connection, "40000000-0000-0000-0000-000000000009",
                    "github/TavallStudios/tavall-mc/issues/9999", "tavall-pvp", "ACTIVE",
                    "Existing game task stays put");
            assertEquals(1, count(connection, "SELECT count(*) FROM task_board_assignment WHERE task_id = "
                    + "'40000000-0000-0000-0000-000000000001'"));
            assertEquals(1, count(connection, "SELECT count(*) FROM task_board_evidence WHERE task_id = "
                    + "'40000000-0000-0000-0000-000000000001'"));
            assertEquals(1, count(connection, "SELECT count(*) FROM task_board_execution_checkpoint WHERE task_id = "
                    + "'40000000-0000-0000-0000-000000000001'"));
            assertEquals(1, count(connection, "SELECT count(*) FROM task_board_execution_checkpoint_payload "
                    + "WHERE checkpoint_id = '40000000-0000-0000-0000-000000000004'"));

            ITaskBoardVentureDataHandler ventureData = new TaskBoardVentureDataHandler(postgres.database());
            ITaskBoardDataHandler taskData = new TaskBoardDataHandler(postgres.database());
            try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
                context.registerBean(ITaskBoardVentureDataHandler.class, () -> ventureData);
                context.registerBean(ITaskBoardDataHandler.class, () -> taskData);
                context.registerBean(TaskBoardVentureQueryHandler.class);
                context.refresh();

                ITaskBoardVentureQueryHandler query = context.getBean(ITaskBoardVentureQueryHandler.class);
                QaSubjectContext manager = ventureManager();
                List<TaskBoardVenture> ventures = query.findVentures(manager);
                assertEquals(28, ventures.size());
                assertEquals(List.of("tavall-web-blog"), query.findVenture("tavall-times-blog", manager)
                        .orElseThrow().ownerRepos());
                assertEquals(List.of("Issue task stays intact"), query.findVentureTasks("tavall-web", manager)
                        .stream().map(org.tavall.qa.domain.TaskBoardTask::title).toList());
                assertEquals(List.of("Re-homed game issue returns"), query.findVentureTasks("tavall-novus", manager)
                        .stream().map(org.tavall.qa.domain.TaskBoardTask::title).toList());
            }
        }
    }

    private static void seedRows(Connection connection) throws Exception {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO task_board_task (task_id, project_key, title, task_status,
                    source_reference, created_at, updated_at, external_key)
                VALUES (?, 'tavall-web', ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
                """)) {
            insert.setString(1, "40000000-0000-0000-0000-000000000001");
            insert.setString(2, "Venture card stays intact");
            insert.setString(3, "BLOCKED");
            insert.setString(4, "https://github.com/TavallStudios/tavall-qa/pull/19");
            insert.setString(5, "venture/tavall-web/venture-card");
            insert.executeUpdate();
            insert.setString(1, "40000000-0000-0000-0000-000000000002");
            insert.setString(2, "Issue task stays intact");
            insert.setString(3, "ACTIVE");
            insert.setString(4, "https://github.com/TavallStudios/tavall-web/issues/106");
            insert.setString(5, "github/TavallStudios/tavall-web/issues/106");
            insert.executeUpdate();
            insert.setString(1, "40000000-0000-0000-0000-000000000008");
            insert.setString(2, "Re-homed game issue returns");
            insert.setString(3, "READY");
            insert.setString(4, "https://github.com/TavallStudios/tavall-mc/issues/401");
            insert.setString(5, "github/TavallStudios/tavall-mc/issues/401");
            insert.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO task_board_task (task_id, project_key, title, task_status,
                    source_reference, created_at, updated_at, external_key)
                VALUES (?, 'tavall-pvp', 'Existing game task stays put', 'ACTIVE', ?,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
                """)) {
            insert.setString(1, "40000000-0000-0000-0000-000000000009");
            insert.setString(2, "https://github.com/TavallStudios/tavall-mc/issues/9999");
            insert.setString(3, "github/TavallStudios/tavall-mc/issues/9999");
            insert.executeUpdate();
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO task_board_assignment (assignment_id, organization_id, task_id,
                        account_id, capability, environment, assigned_by_account_id, reason, assigned_at)
                    VALUES ('40000000-0000-0000-0000-000000000003', 'organization-1',
                        '40000000-0000-0000-0000-000000000001', 'account-1', 'SUBMIT_EVIDENCE',
                        'DEV', 'account-2', 'migration preservation fixture', CURRENT_TIMESTAMP)
                    """);
            statement.execute("""
                    INSERT INTO task_board_evidence (evidence_id, task_id, account_id,
                        qa_classes_at_submission, environment, build_reference, test_case,
                        expected_result, actual_result, evidence_reference, task_fingerprint,
                        evidence_status, submitted_at)
                    VALUES ('40000000-0000-0000-0000-000000000005',
                        '40000000-0000-0000-0000-000000000001', 'account-1', '', 'DEV',
                        'build-1', 'venture migration', 'task preserved', 'task preserved',
                        'https://example.test/evidence', repeat('a', 64), 'PENDING_REVIEW', CURRENT_TIMESTAMP)
                    """);
        }
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO task_board_execution_lease_token (lease_id, task_id, run_id,
                        owner_account_id, issued_at, initial_expires_at)
                    VALUES ('40000000-0000-0000-0000-000000000006',
                        '40000000-0000-0000-0000-000000000001',
                        '40000000-0000-0000-0000-000000000007', 'account-1', CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP + INTERVAL '1 hour')
                    """);
            statement.execute("""
                    INSERT INTO task_board_execution_lease (task_id, run_id, lease_id, owner_account_id,
                        revision, checkpoint_sequence, task_fingerprint, expires_at)
                    VALUES ('40000000-0000-0000-0000-000000000001',
                        '40000000-0000-0000-0000-000000000007',
                        '40000000-0000-0000-0000-000000000006', 'account-1', 1, 1,
                        repeat('b', 64), CURRENT_TIMESTAMP + INTERVAL '1 hour')
                    """);
            statement.execute("""
                    INSERT INTO task_board_execution_checkpoint (checkpoint_id, task_id, run_id, lease_id,
                        checkpoint_sequence, task_fingerprint, source_repository, source_branch,
                        source_commit, diff_sha256, payload_sha256, provider_id, execution_status,
                        recorded_by_account_id, recorded_at)
                    VALUES ('40000000-0000-0000-0000-000000000004',
                        '40000000-0000-0000-0000-000000000001',
                        '40000000-0000-0000-0000-000000000007',
                        '40000000-0000-0000-0000-000000000006', 1, repeat('b', 64),
                        'TavallStudios/tavall-web', 'working/test', repeat('c', 40), repeat('d', 64),
                        repeat('e', 64), 'codex', 'RUNNING', 'account-1', CURRENT_TIMESTAMP)
                    """);
            statement.execute("""
                    INSERT INTO task_board_execution_checkpoint_payload (checkpoint_id, payload_sha256, payload_json)
                    VALUES ('40000000-0000-0000-0000-000000000004', repeat('e', 64), '{}')
                    """);
        }
        connection.commit();
        connection.setAutoCommit(true);
    }

    private static void applyVentureMigration(Connection connection) throws Exception {
        try (var input = VentureMigrationPostgresIntegrationTest.class.getClassLoader()
                .getResourceAsStream("schema/postgres/042_task_board_ventures.sql")) {
            assertNotNull(input, "Missing migration 042 resource");
            try (Statement statement = connection.createStatement()) {
                statement.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    private static int count(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getInt(1);
        }
    }

    private static QaSubjectContext ventureManager() {
        return new QaSubjectContext(java.util.Optional.of(java.util.UUID.fromString("40000000-0000-0000-0000-000000000010")),
                QaSubjectStatus.ACTIVE, java.util.Optional.empty(),
                java.util.Optional.of(java.util.UUID.fromString("40000000-0000-0000-0000-000000000011")),
                true, false, java.util.Set.of(), java.util.Optional.empty(), java.util.Set.of(),
                java.util.Set.of(QaCapability.MANAGE_PROJECTS), List.of(), false);
    }

    private static void assertTask(Connection connection, String taskId, String externalKey,
                                   String status, String title) throws Exception {
        assertTaskInProject(connection, taskId, externalKey, "tavall-engineering", status, title);
    }

    private static void assertTaskInProject(Connection connection, String taskId, String externalKey,
                                            String projectKey, String status, String title) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT project_key, external_key, task_status, title
                FROM task_board_task WHERE task_id = ?
                """)) {
            statement.setString(1, taskId);
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(projectKey, result.getString("project_key"));
                assertEquals(externalKey, result.getString("external_key"));
                assertEquals(status, result.getString("task_status"));
                assertEquals(title, result.getString("title"));
            }
        }
    }
}
