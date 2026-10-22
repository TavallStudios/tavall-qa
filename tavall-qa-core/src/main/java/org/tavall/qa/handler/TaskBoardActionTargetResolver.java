package org.tavall.qa.handler;

import org.springframework.stereotype.Component;
import org.tavall.qa.domain.TaskBoardActionType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public final class TaskBoardActionTargetResolver implements ITaskBoardActionTargetResolver {
    private static final Pattern REPOSITORY_TARGET = Pattern.compile(
            "^([A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+)@([^:]+):(.+)$"
    );

    @Override
    public String normalizeTarget(TaskBoardActionType type, String target) {
        Objects.requireNonNull(type, "type");
        String normalized = requireText(target, "target");
        switch (type) {
            case ADMIN_ROUTE -> requireAdminRoute(normalized);
            case REPOSITORY_FILE -> requireRepositoryTarget(normalized);
            case BUILDER_STUDIO -> requireBuilderTarget(normalized);
            case DISCORD_THREAD -> requireDiscordTarget(normalized);
            case EXTERNAL_LINK -> requireHttpsTarget(normalized);
        }
        return normalized;
    }

    @Override
    public String resolveHref(TaskBoardActionType type, String target) {
        String normalized = normalizeTarget(type, target);
        return switch (type) {
            case ADMIN_ROUTE -> normalized;
            case REPOSITORY_FILE -> repositoryHref(normalized);
            case BUILDER_STUDIO -> "/admin/builder-studio?target="
                    + URLEncoder.encode(normalized, StandardCharsets.UTF_8);
            case DISCORD_THREAD, EXTERNAL_LINK -> normalized;
        };
    }

    private void requireAdminRoute(String target) {
        if (!target.startsWith("/admin/") && !target.startsWith("/builder-studio/")) {
            throw new IllegalArgumentException(
                    "ADMIN_ROUTE targets must stay under /admin/ or /builder-studio/"
            );
        }
    }

    private void requireRepositoryTarget(String target) {
        Matcher matcher = REPOSITORY_TARGET.matcher(target);
        if (!matcher.matches() || matcher.group(3).contains("..")) {
            throw new IllegalArgumentException(
                    "REPOSITORY_FILE targets use owner/repo@ref:path without parent traversal"
            );
        }
    }

    private void requireBuilderTarget(String target) {
        if (target.contains("..")) {
            throw new IllegalArgumentException(
                    "BUILDER_STUDIO targets must not contain parent traversal"
            );
        }
    }

    private void requireDiscordTarget(String target) {
        if (!target.startsWith("https://discord.com/")
                && !target.startsWith("https://canary.discord.com/")) {
            throw new IllegalArgumentException(
                    "DISCORD_THREAD targets must use an HTTPS Discord URL"
            );
        }
    }

    private void requireHttpsTarget(String target) {
        if (!target.startsWith("https://")) {
            throw new IllegalArgumentException(
                    "EXTERNAL_LINK targets must use HTTPS"
            );
        }
    }

    private String repositoryHref(String target) {
        Matcher matcher = REPOSITORY_TARGET.matcher(target);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid repository target");
        }
        String repository = matcher.group(1);
        String ref = matcher.group(2);
        String path = matcher.group(3);
        return "https://github.com/" + repository + "/blob/" + ref + "/" + path;
    }

    private String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
