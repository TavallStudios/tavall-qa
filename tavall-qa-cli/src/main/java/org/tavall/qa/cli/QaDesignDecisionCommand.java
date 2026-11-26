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
import org.tavall.qa.cli.client.QaCliDesignReviewSnapshot;
import org.tavall.qa.domain.TaskBoardDesignCandidate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Canonical {@code qa design decision} CLI adapter reading the attributed review decision. */
@DelegatesTo(IQaDesignDecisionCommand.class)
public final class QaDesignDecisionCommand extends CLICommand<QaCliDesignReviewSnapshot>
        implements IQaDesignDecisionCommand, DependencyAccess<IQaExecutionApiClient> {

    public QaDesignDecisionCommand() {
        super(CLICommandBuilder.create("qa", "design", "decision")
                .description("Read the attributed design-review decision for a QA task.")
                .option(CLICommandOption.required("task-id", "QA task UUID", UUID.class))
                .option(CLICommandOption.optional("credential",
                        "Path to the QA agent credential JSON; defaults to the orchestrator agent file"))
                .build());
    }

    @Override
    protected QaCliDesignReviewSnapshot handle(CLICommandInput input) {
        return getInstance().getDesignReview(input.option("task-id"), QaCliCommandSupport.credentialFile(input))
                .orElse(null);
    }

    @Override
    protected CLICommandResult mapResult(QaCliDesignReviewSnapshot review) {
        if (review == null) {
            return CLICommandResult.failure(1, "DESIGN_REVIEW_UNAVAILABLE",
                    "design review not found or unavailable for this Account");
        }
        Map<String, String> data = new LinkedHashMap<>();
        data.put("taskId", review.taskId().toString());
        data.put("decided", Boolean.toString(review.decided()));
        if (review.winnerKey() != null) {
            data.put("winnerKey", review.winnerKey());
        }
        if (review.decidedByAccountId() != null) {
            data.put("decidedByAccountId", review.decidedByAccountId());
        }
        if (review.decidedAt() != null) {
            data.put("decidedAt", review.decidedAt());
        }
        data.put("note", review.note());
        data.put("revision", Integer.toString(review.revision()));
        data.put("candidates", review.candidates().stream()
                .map(TaskBoardDesignCandidate::key)
                .collect(Collectors.joining(",")));
        return CLICommandResult.success("Read the design review for task " + review.taskId() + ".", data, review);
    }

    @Override
    protected CLICommandResult mapException(Exception exception) {
        if (exception instanceof QaCliApiClientException apiFailure) {
            return CLICommandResult.failure(1, "QA_API_ERROR", apiFailure.getMessage());
        }
        return super.mapException(exception);
    }
}
