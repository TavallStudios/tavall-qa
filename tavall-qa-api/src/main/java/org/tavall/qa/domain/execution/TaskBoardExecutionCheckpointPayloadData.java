package org.tavall.qa.domain.execution;

import java.util.List;
import java.util.Objects;

/** Provider-neutral continuation context; permission observations must be revalidated before execution. */
public record TaskBoardExecutionCheckpointPayloadData(
        int schemaVersion,
        String instructions,
        List<String> acceptanceConditions,
        TaskBoardExecutionDiffData diff,
        List<TaskBoardExecutionReferenceData> documents,
        List<TaskBoardExecutionFileData> inspectedFiles,
        List<TaskBoardExecutionFileData> modifiedFiles,
        List<TaskBoardExecutionOperationData> operations,
        List<TaskBoardExecutionReferenceData> evidence,
        List<String> agentRoles,
        List<TaskBoardExecutionReferenceData> skills,
        List<TaskBoardExecutionReferenceData> policySections,
        List<TaskBoardExecutionReferenceData> permissions,
        List<String> requiredCapabilities,
        List<String> knownErrors,
        String nextAction
) {
    public TaskBoardExecutionCheckpointPayloadData {
        Objects.requireNonNull(instructions, "instructions");
        Objects.requireNonNull(diff, "diff");
        Objects.requireNonNull(nextAction, "nextAction");
        acceptanceConditions = List.copyOf(acceptanceConditions);
        documents = List.copyOf(documents);
        inspectedFiles = List.copyOf(inspectedFiles);
        modifiedFiles = List.copyOf(modifiedFiles);
        operations = List.copyOf(operations);
        evidence = List.copyOf(evidence);
        agentRoles = List.copyOf(agentRoles);
        skills = List.copyOf(skills);
        policySections = List.copyOf(policySections);
        permissions = List.copyOf(permissions);
        requiredCapabilities = requiredCapabilities.stream().distinct().sorted().toList();
        knownErrors = List.copyOf(knownErrors);
        if (schemaVersion != 1 || instructions.isBlank() || instructions.length() > 131_072 || nextAction.isBlank() || nextAction.length() > 4096
                || inspectedFiles.size() > 512 || modifiedFiles.size() > 512 || operations.size() > 512
                || documents.size() > 256 || skills.size() > 256 || policySections.size() > 256 || permissions.size() > 256
                || evidence.size() > 256 || acceptanceConditions.size() > 256 || knownErrors.size() > 128
                || agentRoles.size() > 64 || requiredCapabilities.size() > 256
                || acceptanceConditions.stream().anyMatch(value -> value.isBlank() || value.length() > 4096)
                || knownErrors.stream().anyMatch(value -> value.isBlank() || value.length() > 4096)
                || agentRoles.stream().anyMatch(value -> !value.matches("[a-z][a-z0-9-]{0,95}"))
                || requiredCapabilities.stream().anyMatch(value -> !value.matches("[A-Za-z0-9][A-Za-z0-9._:/-]{0,191}"))) {
            throw new IllegalArgumentException("Checkpoint continuation context exceeds its typed bounds");
        }
    }
}
