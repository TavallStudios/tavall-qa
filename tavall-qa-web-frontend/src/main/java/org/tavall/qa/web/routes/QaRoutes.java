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
    public static final String DESIGN_REVIEW = WORKSPACE_TASK + "/design-review";
    public static final String DESIGN_REVIEW_DECISION = TASK + "/design-review/decision";
    public static final String MESSAGES = "/messages/{key}";
    public static final String VENTURES = "/ventures";
    public static final String VENTURE = "/ventures/{ventureKey}";

    // Server-side mutations
    public static final String PROJECTS_MUTATION = "/projects";
    public static final String TASKS_MUTATION = "/projects/{projectKey}/tasks";
    public static final String TASK_MUTATION = "/projects/{projectKey}/tasks/{taskId}";
    public static final String ACTIONS_MUTATION = "/projects/{projectKey}/tasks/{taskId}/actions";
    public static final String COMMENTS_MUTATION = "/projects/{projectKey}/tasks/{taskId}/comments";
    public static final String EVIDENCE_SUBMISSIONS_MUTATION = "/projects/{projectKey}/tasks/{taskId}/evidence";
    public static final String EVIDENCE_REVIEW_MUTATION = "/projects/{projectKey}/tasks/{taskId}/evidence/{evidenceId}/review";
    public static final String TASK_ACCEPT_MUTATION = "/projects/{projectKey}/tasks/{taskId}/accept";
    public static final String HUMAN_VERIFY_MUTATION = "/projects/{projectKey}/tasks/{taskId}/verify";
    public static final String TASK_REJECT_MUTATION = "/projects/{projectKey}/tasks/{taskId}/reject";
    public static final String ASSIGNMENTS_MUTATION = "/projects/{projectKey}/tasks/{taskId}/assignments";
    public static final String ASSIGNMENT_REVOKE_MUTATION = "/projects/{projectKey}/tasks/{taskId}/assignments/{assignmentId}/revoke";
    public static final String EXECUTION_RESUME_MUTATION = "/projects/{projectKey}/tasks/{taskId}/resume";
    public static final String EXECUTION_RELEASE_MUTATION = "/projects/{projectKey}/tasks/{taskId}/lease/release";
    public static final String EXECUTION_CANCEL_MUTATION = EXECUTION_RELEASE_MUTATION;


    // Legacy paths
    public static final String LEGACY_ROOT = "/task-board";
    public static final String LEGACY_PROJECT = "/task-board/{projectKey}";
    public static final String LEGACY_TASK = "/task-board/{projectKey}/tasks/{taskId}";

    // External navigation
    public static final String COMPANY = "https://tavall.org/";
    public static final String DASHBOARD = "https://dashboard.tavall.org/";
    public static final String MC = "https://mc.tavall.org/";
    public static final String DOCS = "https://docs.tavall.org/";
    public static final String QA = "https://qa.tavall.org/";
    public static final String VENTURES_SITE = "https://ventures.tavall.org/";

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

    public static String venture(String ventureKey) {
        Objects.requireNonNull(ventureKey, "ventureKey");
        return "/ventures/" + ventureKey.trim();
    }

    /** Cross-host link from {@code ventures.tavall.org} back to a project board on {@code qa.tavall.org}. */
    public static String qaProject(String projectKey) {
        return QA + project(projectKey).substring(1);
    }

    /** Cross-host link from {@code ventures.tavall.org} back to a task board on {@code qa.tavall.org}. */
    public static String qaTask(String projectKey, UUID taskId) {
        return QA + task(projectKey, taskId).substring(1);
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

    public static String designReview(String projectKey, UUID taskId) {
        return workspaceTask(projectKey, taskId) + "/design-review";
    }

    public static String designReviewDecision(String projectKey, UUID taskId) {
        return task(projectKey, taskId) + "/design-review/decision";
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

    public static String executionResume(String projectKey, UUID taskId) {
        Objects.requireNonNull(projectKey, "projectKey");
        Objects.requireNonNull(taskId, "taskId");
        return "/projects/" + projectKey.trim() + "/tasks/" + taskId + "/resume";
    }

    public static String executionRelease(String projectKey, UUID taskId) {
        Objects.requireNonNull(projectKey, "projectKey");
        Objects.requireNonNull(taskId, "taskId");
        return "/projects/" + projectKey.trim() + "/tasks/" + taskId + "/lease/release";
    }

    public static String executionCancel(String projectKey, UUID taskId) {
        return executionRelease(projectKey, taskId);
    }

    public static String backendExecutionTask(UUID taskId) {
        Objects.requireNonNull(taskId, "taskId");
        return "/api/execution/tasks/" + taskId;
    }

    public static String backendExecutionLease(UUID taskId) {
        Objects.requireNonNull(taskId, "taskId");
        return "/api/execution/tasks/" + taskId + "/lease";
    }

    public static String backendExecutionRelease(UUID taskId) {
        Objects.requireNonNull(taskId, "taskId");
        return "/api/execution/tasks/" + taskId + "/lease/release";
    }

    public static String backendExecutionCheckpoints(UUID taskId) {
        Objects.requireNonNull(taskId, "taskId");
        return "/api/execution/tasks/" + taskId + "/checkpoints";
    }

    public static String backendExecutionResume(UUID taskId) {
        Objects.requireNonNull(taskId, "taskId");
        return "/api/execution/tasks/" + taskId + "/resume";
    }
}
