package org.tavall.qa.handler;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Applies every Task Board PostgreSQL migration, in order, against a real (native) PostgreSQL
 *  process so schema/constraint regressions are caught in CI. Migration 040 (the venture seed) was
 *  originally merged with {@code project_status = 'BUILDING'}, which the
 *  {@code task_board_project_status_check} constraint rejects; this test fails closed on any such
 *  value and proves the seed is idempotent and complete. */
final class TaskBoardMigrationsPostgresIntegrationTest {
    @TempDir
    static Path directory;
    private static TaskBoardPostgresFixture postgres;

    @BeforeAll
    static void start() throws Exception {
        List<String> migrations = new ArrayList<>(TaskBoardPostgresFixture.CORE_MIGRATIONS);
        migrations.add("040_task_board_venture_projects.sql");
        migrations.sort(String::compareTo);
        postgres = new TaskBoardPostgresFixture(directory, migrations);
    }

    @AfterAll
    static void stop() throws Exception {
        if (postgres != null) {
            postgres.close();
        }
    }

    @Test
    void everyMigrationAppliesCleanlyAgainstRealPostgres() throws Exception {
        try (Connection connection = DriverManager.getConnection(postgres.url(), "postgres", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT count(*) FROM task_board_project WHERE project_status "
                             + "NOT IN ('ACTIVE', 'PAUSED', 'SHIPPED', 'ARCHIVED')")) {
            result.next();
            assertEquals(0, result.getInt(1),
                    "No task_board_project row may use a project_status outside the constraint");
        }
    }

    @Test
    void designReviewMigrationCreatesTypedTableAndConstraints() throws Exception {
        try (Connection connection = DriverManager.getConnection(postgres.url(), "postgres", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT count(*) FROM information_schema.columns "
                             + "WHERE table_name = 'task_board_design_review' "
                             + "AND column_name IN ('task_id', 'surface', 'candidates_json', 'revision', "
                             + "'registered_by_account_id', 'registered_at', 'winner_key', "
                             + "'decided_by_account_id', 'decided_at', 'decision_note')")) {
            result.next();
            assertEquals(10, result.getInt(1),
                    "Migration 041 must create all ten design-review columns");
        }
        try (Connection connection = DriverManager.getConnection(postgres.url(), "postgres", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT count(*) FROM pg_constraint WHERE conrelid = "
                             + "'task_board_design_review'::regclass AND contype = 'c' "
                             + "AND conname IN ('task_board_design_candidates_check', "
                             + "'task_board_design_decision_check')")) {
            result.next();
            assertEquals(2, result.getInt(1),
                    "Migration 041 must create both design-review CHECK constraints");
        }
    }

    @Test
    void ventureSeedPopulatesEveryVentureProject() throws Exception {
        try (Connection connection = DriverManager.getConnection(postgres.url(), "postgres", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(ventureProjectCountSql())) {
            result.next();
            assertEquals(26, result.getInt(1), "Migration 040 must seed exactly the 26 venture projects");
        }
    }

    @Test
    void ventureSeedMigrationIsIdempotentOnReapply() throws Exception {
        try (var input = TaskBoardMigrationsPostgresIntegrationTest.class.getClassLoader()
                .getResourceAsStream("schema/postgres/040_task_board_venture_projects.sql")) {
            assertNotNull(input, "Missing migration 040 resource");
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection connection = DriverManager.getConnection(postgres.url(), "postgres", "");
                 Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }
        try (Connection connection = DriverManager.getConnection(postgres.url(), "postgres", "");
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(ventureProjectCountSql())) {
            result.next();
            assertEquals(26, result.getInt(1), "Re-applying 040 must not duplicate venture projects");
        }
    }

    @Test
    void migration043UpgradesAnExistingHumanDecisionAndCanBeReapplied(@TempDir Path upgradeDirectory) throws Exception {
        List<String> beforeAiReview = TaskBoardPostgresFixture.CORE_MIGRATIONS.stream()
                .filter(migration -> !migration.startsWith("043_"))
                .toList();
        String taskId;
        try (var upgrade = new TaskBoardPostgresFixture(upgradeDirectory, beforeAiReview)) {
            try (var composition = new TaskBoardPostgresComposition(upgrade.database(), java.time.Clock.systemUTC())) {
                var mutations = composition.bean(ITaskBoardMutationHandler.class);
                var owner = org.tavall.qa.auth.TaskBoardTestSubjects.manager(java.util.UUID.randomUUID(), java.util.UUID.randomUUID());
                var task = mutations.createTask(owner, new org.tavall.qa.request.TaskBoardTaskCreateRequest(
                        "tavall-engineering", "legacy-design", "Legacy design", "Legacy design", "", "",
                        org.tavall.qa.domain.TaskBoardTaskPriority.HIGH, org.tavall.qa.domain.TaskBoardAudience.STAFF,
                        1, false, false, "test", "issue://23", 0));
                taskId = task.taskId().toString();
            }
            try (var connection = DriverManager.getConnection(upgrade.url(), "postgres", "");
                 var insert = connection.prepareStatement("""
                         INSERT INTO task_board_design_review
                           (task_id, surface, candidates_json, revision, registered_by_account_id, registered_at,
                            winner_key, decided_by_account_id, decided_at, decision_note)
                         VALUES (?, 'Dashboard', '[{},{}]', 2, ?, now(), 'B', ?, now(), 'Owner selected B')
                         """);
                 var resource = getClass().getClassLoader()
                         .getResourceAsStream("schema/postgres/043_task_board_design_review_ai_review.sql")) {
                String actor = java.util.UUID.randomUUID().toString();
                insert.setString(1, taskId);
                insert.setString(2, actor);
                insert.setString(3, actor);
                assertEquals(1, insert.executeUpdate());
                assertNotNull(resource);
                String sql = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
                try (var statement = connection.createStatement()) {
                    statement.execute(sql);
                    statement.execute(sql);
                    try (var row = statement.executeQuery("""
                            SELECT winner_key, decision_note, revision, ai_reviewer_model,
                                   ai_evidence_refs, ai_artifact_refs
                            FROM task_board_design_review
                            """)) {
                        assertTrue(row.next());
                        assertEquals("B", row.getString("winner_key"));
                        assertEquals("Owner selected B", row.getString("decision_note"));
                        assertEquals(2, row.getInt("revision"));
                        assertNull(row.getString("ai_reviewer_model"));
                        assertEquals("[]", row.getString("ai_evidence_refs"));
                        assertEquals("[]", row.getString("ai_artifact_refs"));
                    }
                }
            }
        }
    }

    private static String ventureProjectCountSql() {
        return "SELECT count(*) FROM task_board_project WHERE project_key IN ("
                + "'tavall-web','tavall-times-blog','tavall-account','tavall-dashboard','tavall-commerce',"
                + "'tavall-docs','tavall-qa','tavall-content','tavall-cloud','tavall-ai','tavall-ai-chat',"
                + "'tavall-ai-plugin','tavall-mcp','tavall-analytics','tavall-workflows','tavall-contractors',"
                + "'tavall-couriers','tavall-builder','tavall-affiliates','tavall-labs','tavall-ops',"
                + "'tavall-mc-speedrun','tavall-webstore','tavall-hytale','tavall-roblox','tavall-open-harness')";
    }
}
