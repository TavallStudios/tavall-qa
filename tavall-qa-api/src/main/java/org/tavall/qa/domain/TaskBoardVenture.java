package org.tavall.qa.domain;

import java.util.List;
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
        String goalFamilyRef
) {
    public TaskBoardVenture {
        slug = requireText(slug, "slug");
        sourceTaskKey = requireText(sourceTaskKey, "sourceTaskKey");
        purpose = requireText(purpose, "purpose");
        ownerRepos = normalized(ownerRepos, "ownerRepos");
        stage = requireText(stage, "stage");
        liveUrls = normalized(liveUrls, "liveUrls");
        topBlocker = requireText(topBlocker, "topBlocker");
        goalFamilyRef = goalFamilyRef == null ? null : requireText(goalFamilyRef, "goalFamilyRef");
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
