package org.tavall.qa.cli;

import org.tavall.cli.api.command.CLICommand;
import org.tavall.cli.api.command.builder.CLICommandBuilder;
import org.tavall.cli.api.command.data.CLICommandInput;
import org.tavall.cli.api.command.data.CLICommandResult;
import org.tavall.cli.api.command.option.CLICommandOption;
import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.qa.cli.client.IQaExecutionApiClient;
import org.tavall.qa.cli.client.QaCliApiClientException;
import org.tavall.qa.cli.client.QaCliSecretRedactor;
import org.tavall.qa.cli.client.QaCliTaskUpsertRequest;
import org.tavall.qa.cli.client.QaCliTaskUpsertResult;
import org.tavall.qa.cli.github.IGithubIssueReader;
import org.tavall.qa.cli.github.QaGithubIssue;
import org.tavall.qa.cli.github.QaGithubIssueQuery;
import org.tavall.qa.cli.github.QaGithubReaderException;
import org.tavall.qa.cli.github.QaGithubSyncMapping;
import org.tavall.qa.cli.github.QaGithubSyncResult;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Canonical {@code qa github sync} CLI adapter.
 *
 * <p>Reads GitHub issues through the typed {@link IGithubIssueReader} seam and upserts one QA task
 * per issue through the typed {@link IQaExecutionApiClient}, keyed
 * {@code github/<owner>/<repo>/issues/<n>}. The mapping is deterministic
 * ({@link QaGithubSyncMapping}); {@code --dry-run} reports the planned keys without calling the QA
 * API, and a single issue failure is recorded instead of aborting the batch.</p>
 */
@DelegatesTo(IQaGithubSyncCommand.class)
public final class QaGithubSyncCommand extends CLICommand<QaGithubSyncResult>
        implements IQaGithubSyncCommand, DependencyAccess<IGithubIssueReader, IQaExecutionApiClient> {
    public static final String DEFAULT_OWNER = "TavallStudios";
    public static final String DEFAULT_STATE = QaGithubIssueQuery.DEFAULT_STATE;
    public static final int DEFAULT_LIMIT = QaGithubIssueQuery.DEFAULT_LIMIT;
    public static final String DEFAULT_SECTION = QaTaskUpsertCommand.DEFAULT_SECTION;
    public static final String FAILURE_REASON = "GITHUB_SYNC_INCOMPLETE";
    public static final String READER_FAILURE_REASON = "GITHUB_READER_ERROR";
    public static final String API_FAILURE_REASON = "QA_API_ERROR";

    public QaGithubSyncCommand() {
        super(CLICommandBuilder.create("qa", "github", "sync")
                .description("Sync GitHub issues from one or more repositories into the QA task board.")
                .option(CLICommandOption.optional("owner", "GitHub owner (organization) name", DEFAULT_OWNER))
                .option(CLICommandOption.required("repos",
                        "Comma-separated repository names, e.g. tavall-ai,tavall-docs,tavall-web"))
                .option(CLICommandOption.optional("state",
                        "Issue state filter (open, closed, all)", DEFAULT_STATE))
                .option(CLICommandOption.flag("dry-run",
                        "Report the planned task keys without calling the QA API"))
                .option(CLICommandOption.optional("limit",
                        "Maximum issues read per repository", Integer.class, DEFAULT_LIMIT))
                .option(CLICommandOption.optional("project",
                        "QA project key; defaults to " + QaTaskUpsertCommand.PROJECT_ENVIRONMENT + " or "
                                + QaTaskUpsertCommand.DEFAULT_PROJECT,
                        QaTaskUpsertCommand.defaultProject()))
                .option(CLICommandOption.optional("section", "QA section key", DEFAULT_SECTION))
                .option(CLICommandOption.optional("credential",
                        "Path to the QA agent credential JSON; defaults to the orchestrator agent file"))
                .build());
    }

    @Override
    protected QaGithubSyncResult handle(CLICommandInput input) {
        String owner = input.<String>findOption("owner").orElse(DEFAULT_OWNER);
        List<String> repositories = parseRepositories(input.option("repos"));
        String stateFilter = input.<String>findOption("state").orElse(DEFAULT_STATE).trim().toLowerCase(Locale.ROOT);
        if (!QaGithubIssueQuery.STATES.contains(stateFilter)) {
            throw new IllegalArgumentException("--state must be one of open, closed, all");
        }
        int limit = input.<Integer>findOption("limit").orElse(DEFAULT_LIMIT);
        boolean dryRun = input.<Boolean>findOption("dry-run").orElse(false);
        String section = input.<String>findOption("section").orElse(DEFAULT_SECTION);
        String project = input.<String>findOption("project").orElse(QaTaskUpsertCommand.defaultProject());
        Path credentialFile = QaCliCommandSupport.credentialFile(input);

        List<String> syncedKeys = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int scanned = 0;
        int created = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;

        for (String repository : repositories) {
            List<QaGithubIssue> issues = getInstance().githubIssueReader()
                    .readIssues(new QaGithubIssueQuery(owner, repository, stateFilter, limit));
            scanned += issues.size();
            for (QaGithubIssue issue : issues) {
                Optional<TaskBoardTaskStatus> status = QaGithubSyncMapping.status(issue);
                if (status.isEmpty()) {
                    skipped++;
                    errors.add(issue.externalKey() + ": unsupported issue state '" + issue.state() + "'");
                    continue;
                }
                if (dryRun) {
                    syncedKeys.add(issue.externalKey());
                    continue;
                }
                try {
                    QaCliTaskUpsertResult result = getInstance().qaExecutionApiClient()
                            .upsertTask(taskRequest(issue, status.get(), section, project, credentialFile));
                    if (result.created()) {
                        created++;
                    } else {
                        updated++;
                    }
                    syncedKeys.add(issue.externalKey());
                } catch (RuntimeException issueFailure) {
                    failed++;
                    errors.add(issue.externalKey() + ": " + redactedMessage(issueFailure));
                }
            }
        }

        return new QaGithubSyncResult(owner, repositories, stateFilter, dryRun, scanned, created, updated,
                skipped, failed, syncedKeys, errors);
    }

    @Override
    protected CLICommandResult mapResult(QaGithubSyncResult result) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("owner", result.owner());
        data.put("repositories", String.join(",", result.repositories()));
        data.put("stateFilter", result.stateFilter());
        data.put("dryRun", Boolean.toString(result.dryRun()));
        data.put("scanned", Integer.toString(result.scanned()));
        data.put("created", Integer.toString(result.created()));
        data.put("updated", Integer.toString(result.updated()));
        data.put("skipped", Integer.toString(result.skipped()));
        data.put("failed", Integer.toString(result.failed()));
        data.put("syncedKeys", String.join(",", result.syncedKeys()));
        if (!result.errors().isEmpty()) {
            data.put("errors", String.join("; ", result.errors()));
        }
        String summary = summary(result);
        if (!result.successful()) {
            return CLICommandResult.failure(1, FAILURE_REASON, summary, data);
        }
        return CLICommandResult.success(summary, data, result);
    }

    @Override
    protected CLICommandResult mapException(Exception exception) {
        if (exception instanceof QaGithubReaderException readerFailure) {
            return CLICommandResult.failure(1, READER_FAILURE_REASON,
                    QaCliSecretRedactor.redact(readerFailure.getMessage()));
        }
        if (exception instanceof QaCliApiClientException apiFailure) {
            return CLICommandResult.failure(1, API_FAILURE_REASON, apiFailure.getMessage());
        }
        return super.mapException(exception);
    }

    /**
     * Parses the required comma-separated {@code --repos} option into unique repository names.
     */
    static List<String> parseRepositories(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("--repos must list at least one repository");
        }
        LinkedHashSet<String> repositories = new LinkedHashSet<>();
        for (String candidate : raw.split(",")) {
            String name = candidate.trim();
            if (name.isEmpty()) {
                continue;
            }
            if (!QaGithubIssue.isValidName(name)) {
                throw new IllegalArgumentException("--repos contains an invalid repository name: '" + name + "'");
            }
            repositories.add(name);
        }
        if (repositories.isEmpty()) {
            throw new IllegalArgumentException("--repos must list at least one repository");
        }
        return List.copyOf(repositories);
    }

    private static QaCliTaskUpsertRequest taskRequest(
            QaGithubIssue issue,
            TaskBoardTaskStatus status,
            String section,
            String project,
            Path credentialFile) {
        return new QaCliTaskUpsertRequest(
                issue.externalKey(),
                issue.title().isBlank() ? issue.externalKey() : issue.title(),
                status,
                QaGithubSyncMapping.description(issue),
                QaGithubSyncMapping.priority(issue),
                section,
                project,
                QaGithubSyncMapping.SOURCE_TYPE,
                issue.sourceReference(),
                credentialFile);
    }

    private static String redactedMessage(RuntimeException failure) {
        String message = failure.getMessage();
        String detail = message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
        return QaCliSecretRedactor.redact(detail);
    }

    private static String summary(QaGithubSyncResult result) {
        String repositories = String.join(",", result.repositories());
        if (result.dryRun()) {
            return "Dry run: planned " + result.syncedKeys().size() + " " + result.stateFilter()
                    + " GitHub issues from " + result.owner() + "/" + repositories
                    + " (no QA API calls made)";
        }
        return "Synced " + result.stateFilter() + " GitHub issues from " + result.owner() + "/" + repositories
                + ": scanned=" + result.scanned()
                + ", created=" + result.created()
                + ", updated=" + result.updated()
                + ", skipped=" + result.skipped()
                + ", failed=" + result.failed();
    }
}
