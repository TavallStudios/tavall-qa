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

    private static String ventureProjectCountSql() {
        return "SELECT count(*) FROM task_board_project WHERE project_key IN ("
                + "'tavall-web','tavall-times-blog','tavall-account','tavall-dashboard','tavall-commerce',"
                + "'tavall-docs','tavall-qa','tavall-content','tavall-cloud','tavall-ai','tavall-ai-chat',"
                + "'tavall-ai-plugin','tavall-mcp','tavall-analytics','tavall-workflows','tavall-contractors',"
                + "'tavall-couriers','tavall-builder','tavall-affiliates','tavall-labs','tavall-ops',"
                + "'tavall-mc-speedrun','tavall-webstore','tavall-hytale','tavall-roblox','tavall-open-harness')";
    }
}
