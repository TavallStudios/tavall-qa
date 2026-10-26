package org.tavall.qa.handler;

import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.database.postgres.PostgresDatabaseBuilder;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Test-only isolated PostgreSQL process; no host database or environment credentials are used. */
final class TaskBoardExecutionPostgresFixture implements AutoCloseable {
    private final Path root;
    private final Path binaries;
    private final String url;
    private IPostgresDatabase database;

    TaskBoardExecutionPostgresFixture(Path root) throws Exception {
        this.root = root;
        binaries = Path.of(System.getProperty("tavall.qa.postgres.bin", "/usr/lib/postgresql/14/bin"));
        boolean installed = Files.isExecutable(binaries.resolve("initdb")) && Files.isExecutable(binaries.resolve("pg_ctl"));
        if (Boolean.getBoolean("tavall.qa.checkpoint.postgres.required")) {
            assertTrue(installed, "Checkpoint integration requires native PostgreSQL fixture binaries");
        } else {
            assumeTrue(installed, "Native PostgreSQL fixture unavailable; this does not prove persistence acceptance");
        }
        int port;
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            port = socket.getLocalPort();
        }
        url = "jdbc:postgresql://127.0.0.1:" + port + "/postgres";
        try {
            command("initdb", "-D", root.resolve("data").toString(), "--auth=trust", "--username=postgres", "--no-locale", "--encoding=UTF8");
            Files.writeString(root.resolve("data/postgresql.conf"), "\nlisten_addresses='127.0.0.1'\nport=" + port
                    + "\nunix_socket_directories=''\nshared_buffers='16MB'\nmax_connections=24\n",
                    StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
            start();
            try (var connection = DriverManager.getConnection(url, "postgres", "");
                 var statement = connection.createStatement()) {
                for (String migration : List.of("027_task_board.sql", "028_task_board_qa_workflows.sql",
                        "034_task_board_execution_checkpoints.sql")) {
                    try (var input = getClass().getClassLoader().getResourceAsStream("schema/postgres/" + migration)) {
                        if (input == null) throw new IllegalStateException("Missing migration " + migration);
                        statement.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
            }
            openDatabase();
        } catch (Exception | Error failure) {
            try { close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    IPostgresDatabase database() { return database; }

    String url() { return url; }

    void restart() throws Exception {
        database.close();
        database = null;
        command("pg_ctl", "-D", root.resolve("data").toString(), "-m", "fast", "-w", "stop");
        start();
        openDatabase();
    }

    private void start() throws Exception {
        command("pg_ctl", "-D", root.resolve("data").toString(), "-l", root.resolve("postgres.log").toString(), "-w", "-t", "30", "start");
    }

    private void openDatabase() {
        database = PostgresDatabaseBuilder.create().jdbcUrl(url).username("postgres").password("")
                .persistenceUnitName("tavall-qa-execution-checkpoint-integration")
                .entityPackage("org.tavall.qa.persistence.entity").generateSchema(false).showSql(false).build()
                .orElseThrow(() -> new IllegalStateException("Mapped checkpoint database did not start"));
    }

    @Override
    public void close() throws Exception {
        if (database != null) {
            database.close();
            database = null;
        }
        if (Files.exists(root.resolve("data/postmaster.pid"))) {
            command("pg_ctl", "-D", root.resolve("data").toString(), "-m", "immediate", "-w", "stop");
        }
    }

    private void command(String executable, String... arguments) throws Exception {
        List<String> command = new java.util.ArrayList<>(List.of(binaries.resolve(executable).toString()));
        command.addAll(List.of(arguments));
        Path output = Files.createTempFile(root, executable + "-", ".log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        if (!process.waitFor(Duration.ofSeconds(40).toMillis(), TimeUnit.MILLISECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("PostgreSQL fixture command timed out: " + executable);
        }
        if (process.exitValue() != 0) {
            String diagnostic = Files.readString(output);
            throw new IllegalStateException("PostgreSQL fixture " + executable + " failed: "
                    + diagnostic.substring(Math.max(0, diagnostic.length() - 3000)));
        }
    }
}
