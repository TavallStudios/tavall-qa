package org.tavall.qa.cli;

import org.tavall.cli.api.command.CLICommand;
import org.tavall.cli.api.command.builder.CLICommandBuilder;
import org.tavall.cli.api.command.data.CLICommandInput;
import org.tavall.cli.api.command.data.CLICommandResult;
import org.tavall.cli.api.command.option.CLICommandOption;
import org.tavall.qa.cli.client.IQaExecutionApiClient;
import org.tavall.qa.cli.client.QaCliApiClientException;
import org.tavall.qa.cli.client.QaCliDesignAiReviewRequest;
import org.tavall.qa.cli.client.QaCliDesignAiReviewResult;
import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.annotations.DelegatesTo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Records a provisional AI design review for a task; only approved models make it deployable. */
@DelegatesTo
public final class QaDesignAiReviewCommand extends CLICommand<QaCliDesignAiReviewResult>
        implements DependencyAccess<IQaExecutionApiClient> {

    public QaDesignAiReviewCommand() {
        super(CLICommandBuilder.create("qa", "design", "ai-review")
                .description("Record a provisional AI design review for a task.")
                .option(CLICommandOption.required("task-id", "QA task UUID", UUID.class))
                .option(CLICommandOption.required("candidate", "Candidate key (A-D)"))
                .option(CLICommandOption.required("rationale", "AI review rationale"))
                .option(CLICommandOption.required("evidence-refs", "Comma-separated evidence refs"))
                .option(CLICommandOption.optional("artifact-refs", "Comma-separated artifact refs"))
                .option(CLICommandOption.optional("screenshot-artifact-ref", "Screenshot artifact ref"))
                .option(CLICommandOption.required("reviewer-model", "Claude Opus or Gemini 4 Neon model identifier"))
                .option(CLICommandOption.required("reviewer-agent-identity", "Reviewer agent identity"))
                .option(CLICommandOption.optional("expected-revision", "Expected review revision", Integer.class, null))
                .option(CLICommandOption.optional("credential", "Path to the QA agent credential JSON"))
                .build());
    }

    @Override
    protected QaCliDesignAiReviewResult handle(CLICommandInput input) {
        return getInstance().recordDesignAiReview(new QaCliDesignAiReviewRequest(
                input.option("task-id"), input.option("candidate"), input.option("rationale"),
                splitRefs(input.option("evidence-refs")), splitRefs(input.<String>findOption("artifact-refs").orElse("")),
                input.<String>findOption("screenshot-artifact-ref").orElse(null), input.option("reviewer-model"),
                input.option("reviewer-agent-identity"), input.<Integer>findOption("expected-revision").orElse(null),
                QaCliCommandSupport.credentialFile(input)));
    }

    @Override
    protected CLICommandResult mapResult(QaCliDesignAiReviewResult result) {
        return CLICommandResult.success("Recorded provisional AI design review for task " + result.taskId() + ".",
                Map.of("taskId", result.taskId().toString(),
                        "revision", Integer.toString(result.revision()),
                        "aiReviewerModel", result.aiReviewerModel(),
                        "aiCandidateKey", result.aiCandidateKey(),
                        "decided", Boolean.toString(result.decided()),
                        "deployable", Boolean.toString(result.deployable())), result);
    }

    private static List<String> splitRefs(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Override
    protected CLICommandResult mapException(Exception exception) {
        if (exception instanceof QaCliApiClientException apiFailure) {
            return CLICommandResult.failure(1, "QA_API_ERROR", apiFailure.getMessage());
        }
        return super.mapException(exception);
    }
}
