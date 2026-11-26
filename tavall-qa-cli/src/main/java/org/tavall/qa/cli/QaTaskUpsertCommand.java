package org.tavall.qa.cli;

import org.tavall.cli.api.command.CLICommand;
import org.tavall.cli.api.command.data.CLICommandInput;
import org.tavall.cli.api.command.data.CLICommandResult;
import org.tavall.cli.api.command.builder.CLICommandBuilder;
import org.tavall.cli.api.command.option.CLICommandOption;
import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.qa.cli.client.IQaExecutionApiClient;
import org.tavall.qa.cli.client.QaCliApiClientException;
import org.tavall.qa.cli.client.QaCliTaskUpsertRequest;
import org.tavall.qa.cli.client.QaCliTaskUpsertResult;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Canonical {@code qa task upsert} CLI adapter; idempotent per external key. */
@DelegatesTo(IQaTaskUpsertCommand.class)
public final class QaTaskUpsertCommand extends CLICommand<QaCliTaskUpsertResult>
        implements IQaTaskUpsertCommand, DependencyAccess<IQaExecutionApiClient> {
    public static final String PROJECT_ENVIRONMENT = "TAVALL_QA_PROJECT";
    public static final String DEFAULT_PROJECT = "tavall-engineering";
    public static final String DEFAULT_SECTION = "orchestration";
    public static final String DEFAULT_SOURCE_TYPE = "github-issue";

    public QaTaskUpsertCommand() {
        super(CLICommandBuilder.create("qa", "task", "upsert")
                .description("Upsert a QA task by external key; re-sending the same body keeps the same task id.")
                .option(CLICommandOption.required("external-key",
                        "External key, e.g. github/<owner>/<repo>/issues/<n>"))
                .option(CLICommandOption.required("title", "Task title"))
                .option(CLICommandOption.required("status",
                        "Task status (BACKLOG, READY, ACTIVE, BLOCKED, REVIEW, DONE, CANCELLED)",
                        TaskBoardTaskStatus.class))
                .option(CLICommandOption.optional("description", "Task description; defaults to the title"))
                .option(CLICommandOption.optional("priority",
                        "Task priority (LOW, NORMAL, HIGH, CRITICAL)",
                        TaskBoardTaskPriority.class, TaskBoardTaskPriority.NORMAL))
                .option(CLICommandOption.optional("section", "QA section key", DEFAULT_SECTION))
                .option(CLICommandOption.optional("project",
                        "QA project key; defaults to TAVALL_QA_PROJECT or " + DEFAULT_PROJECT,
                        defaultProject()))
                .option(CLICommandOption.optional("source-type", "Task source type", DEFAULT_SOURCE_TYPE))
                .option(CLICommandOption.optional("source-reference", "Task source reference"))
                .option(CLICommandOption.optional("credential",
                        "Path to the QA agent credential JSON; defaults to the orchestrator agent file"))
                .build());
    }

    @Override
    protected QaCliTaskUpsertResult handle(CLICommandInput input) {
        Optional<String> description = input.findOption("description");
        Optional<String> sourceReference = input.findOption("source-reference");
        Optional<TaskBoardTaskPriority> priority = input.findOption("priority");
        Optional<String> section = input.findOption("section");
        Optional<String> project = input.findOption("project");
        Optional<String> sourceType = input.findOption("source-type");
        QaCliTaskUpsertRequest request = new QaCliTaskUpsertRequest(
                input.option("external-key"),
                input.option("title"),
                input.option("status"),
                description.orElse(null),
                priority.orElse(TaskBoardTaskPriority.NORMAL),
                section.orElse(DEFAULT_SECTION),
                project.orElse(defaultProject()),
                sourceType.orElse(DEFAULT_SOURCE_TYPE),
                sourceReference.orElse(null),
                QaCliCommandSupport.credentialFile(input));
        return getInstance().upsertTask(request);
    }

    @Override
    protected CLICommandResult mapResult(QaCliTaskUpsertResult result) {
        Map<String, String> data = new LinkedHashMap<>();
        if (result.taskId() != null) {
            data.put("taskId", result.taskId().toString());
        }
        if (result.status() != null) {
            data.put("status", result.status().name());
        }
        data.put("created", Boolean.toString(result.created()));
        data.put("updated", Boolean.toString(result.updated()));
        data.put("externalKey", result.externalKey());
        return CLICommandResult.success("Upserted QA task " + result.externalKey() + ".", data, result);
    }

    @Override
    protected CLICommandResult mapException(Exception exception) {
        if (exception instanceof QaCliApiClientException apiFailure) {
            return CLICommandResult.failure(1, "QA_API_ERROR", apiFailure.getMessage());
        }
        return super.mapException(exception);
    }

    static String defaultProject() {
        String fromEnvironment = System.getenv(PROJECT_ENVIRONMENT);
        return fromEnvironment == null || fromEnvironment.isBlank() ? DEFAULT_PROJECT : fromEnvironment.trim();
    }
}
