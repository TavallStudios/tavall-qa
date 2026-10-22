package org.tavall.qa.web.routes;

import java.util.Objects;
import java.util.UUID;

/**
 * Compile-time route namespace for the Tavall QA and Task Board surface on {@code qa.tavall.org}.
 *
 * <p>The surface is mounted on the dedicated host {@code qa.tavall.org} rooted at {@code /}.
 * Legacy {@code /task-board/**} paths are supported for compatibility and redirection.</p>
 */
public final class QaRoutes {

    public static final String ROOT = "/";
    public static final String PROJECT = "/projects/{projectKey}";
    public static final String TASK = "/projects/{projectKey}/tasks/{taskId}";
    public static final String WORKSPACE = "/workspace/{projectKey}";
    public static final String WORKSPACE_TASK = "/workspace/{projectKey}/tasks/{taskId}";
    public static final String MESSAGES = "/messages/{key}";

    // Mutations
    public static final String PROJECTS_MUTATION = "/projects";
    public static final String TASKS_MUTATION = "/projects/{projectKey}/tasks";
    public static final String TASK_MUTATION = "/projects/{projectKey}/tasks/{taskId}";
    public static final String ACTIONS_MUTATION = "/projects/{projectKey}/tasks/{taskId}/actions";
    public static final String COMMENTS_MUTATION = "/projects/{projectKey}/tasks/{taskId}/comments";

    // Legacy paths
    public static final String LEGACY_ROOT = "/task-board";
    public static final String LEGACY_PROJECT = "/task-board/{projectKey}";
    public static final String LEGACY_TASK = "/task-board/{projectKey}/tasks/{taskId}";

    // External navigation
    public static final String COMPANY = "https://tavall.org/";
    public static final String DASHBOARD = "https://dashboard.tavall.org/";
    public static final String MC = "https://mc.tavall.org/";
    public static final String DOCS = "https://docs.tavall.org/";

    private QaRoutes() {
    }

    public static String project(String projectKey) {
        Objects.requireNonNull(projectKey, "projectKey");
        return "/projects/" + projectKey.trim();
    }

    public static String task(String projectKey, UUID taskId) {
        Objects.requireNonNull(projectKey, "projectKey");
        Objects.requireNonNull(taskId, "taskId");
        return "/projects/" + projectKey.trim() + "/tasks/" + taskId;
    }

    public static String workspace(String projectKey) {
        Objects.requireNonNull(projectKey, "projectKey");
        return "/workspace/" + projectKey.trim();
    }

    public static String workspaceTask(String projectKey, UUID taskId) {
        Objects.requireNonNull(projectKey, "projectKey");
        Objects.requireNonNull(taskId, "taskId");
        return "/workspace/" + projectKey.trim() + "/tasks/" + taskId;
    }

    public static String legacyProject(String projectKey) {
        Objects.requireNonNull(projectKey, "projectKey");
        return "/task-board/" + projectKey.trim();
    }

    public static String legacyTask(String projectKey, UUID taskId) {
        Objects.requireNonNull(projectKey, "projectKey");
        Objects.requireNonNull(taskId, "taskId");
        return "/task-board/" + projectKey.trim() + "/tasks/" + taskId;
    }
}
