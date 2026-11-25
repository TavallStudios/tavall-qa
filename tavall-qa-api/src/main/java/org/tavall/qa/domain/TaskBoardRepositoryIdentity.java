package org.tavall.qa.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Repository identity parsed from a Task Board task source reference.
 *
 * <p>Source references are recorded as GitHub issue URLs, provider-qualified paths such as
 * {@code github/TavallStudios/tavall-qa/issues/38}, or venture keys such as
 * {@code venture/tavall-qa/venture-card}. Matching compares the complete repository segment
 * case-insensitively, so {@code tavall-qa} never matches a merely similar repository name like
 * {@code tavall-web-qa}.</p>
 */
public record TaskBoardRepositoryIdentity(String owner, String name) {

    public TaskBoardRepositoryIdentity {
        owner = requireSegment(owner, "owner");
        name = requireSegment(name, "name");
    }

    /** Extracts the repository identity from a source reference, or empty when it has none. */
    public static Optional<TaskBoardRepositoryIdentity> fromSourceReference(String sourceReference) {
        if (sourceReference == null) {
            return Optional.empty();
        }
        String reference = sourceReference.trim();
        if (reference.isEmpty()) {
            return Optional.empty();
        }
        int scheme = reference.indexOf("://");
        if (scheme >= 0) {
            reference = reference.substring(scheme + 3);
        }
        List<String> segments = Arrays.stream(reference.split("/"))
                .map(String::trim)
                .filter(segment -> !segment.isEmpty())
                .toList();
        if (segments.isEmpty()) {
            return Optional.empty();
        }
        int ownerIndex = isHostOrProviderNamespace(segments.getFirst()) ? 1 : 0;
        if (segments.size() <= ownerIndex + 1) {
            return Optional.empty();
        }
        return Optional.of(new TaskBoardRepositoryIdentity(
                segments.get(ownerIndex),
                segments.get(ownerIndex + 1)
        ));
    }

    /** True when this identity names the supplied owner repository; owner prefixes are allowed. */
    public boolean matches(String repository) {
        if (repository == null) {
            return false;
        }
        String slug = repository.trim();
        int separator = slug.lastIndexOf('/');
        if (separator >= 0) {
            slug = slug.substring(separator + 1);
        }
        return !slug.isEmpty() && name.equalsIgnoreCase(slug);
    }

    private static boolean isHostOrProviderNamespace(String segment) {
        return segment.contains(".") || segment.equalsIgnoreCase("github");
    }

    private static String requireSegment(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
