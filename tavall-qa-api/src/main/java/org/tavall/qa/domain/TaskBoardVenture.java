package org.tavall.qa.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Objects;

/** A venture directory entry, independent of the task board project lifecycle. */
public record TaskBoardVenture(
        String slug,
        String sourceTaskKey,
        String purpose,
        List<String> ownerRepos,
        String stage,
        List<String> liveUrls,
        String topBlocker,
        String goalFamilyRef,
        String displayName,
        UUID sourceTaskId,
        @JsonSerialize(using = ToStringSerializer.class) Instant createdAt,
        @JsonSerialize(using = ToStringSerializer.class) Instant updatedAt
) {
    public TaskBoardVenture {
        slug = requireText(slug, "slug");
        displayName = requireText(displayName, "displayName");
        sourceTaskKey = requireText(sourceTaskKey, "sourceTaskKey");
        purpose = requireText(purpose, "purpose");
        ownerRepos = normalized(ownerRepos, "ownerRepos");
        stage = requireText(stage, "stage");
        liveUrls = normalized(liveUrls, "liveUrls");
        topBlocker = requireText(topBlocker, "topBlocker");
        goalFamilyRef = goalFamilyRef == null ? null : requireText(goalFamilyRef, "goalFamilyRef");
    }

    public TaskBoardVenture(String slug, String sourceTaskKey, String purpose, List<String> ownerRepos,
                            String stage, List<String> liveUrls, String topBlocker, String goalFamilyRef) {
        this(slug, sourceTaskKey, purpose, ownerRepos, stage, liveUrls, topBlocker, goalFamilyRef,
                Arrays.stream(requireText(slug, "slug").split("-"))
                        .filter(part -> !part.isEmpty())
                        .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                        .collect(Collectors.joining(" ")),
                null, null, null);
    }

    /** True when any declared owner repository matches the supplied identity. */
    public boolean owns(TaskBoardRepositoryIdentity repository) {
        Objects.requireNonNull(repository, "repository");
        return ownerRepos.stream().anyMatch(repository::matches);
    }

    private static List<String> normalized(List<String> values, String name) {
        return Objects.requireNonNull(values, name).stream()
                .map(value -> requireText(value, name + " entry"))
                .toList();
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
