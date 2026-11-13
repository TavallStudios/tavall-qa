package org.tavall.qa.web.pages.workspace;

import org.tavall.qa.domain.TaskBoardActionType;
import org.tavall.qa.domain.TaskBoardAssignment;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardCommentType;
import org.tavall.qa.domain.TaskBoardEvidence;
import org.tavall.qa.domain.TaskBoardEvidenceStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskFingerprint;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaTier;
import org.tavall.qa.view.TaskBoardAssignmentView;
import org.tavall.qa.view.TaskBoardAuditView;
import org.tavall.qa.view.TaskBoardEvidenceView;
import org.tavall.qa.view.TaskBoardTaskView;
import org.tavall.qa.web.generated.QaCSS;
import org.tavall.qa.web.pages.shared.QaProduct;
import org.tavall.qa.web.routes.QaRoutes;
import org.tavall.web.api.html.HTMLAttribute;
import org.tavall.web.api.html.HTMLDocument;
import org.tavall.web.api.html.HTMLElement;
import org.tavall.web.frontend.design.Forge;
import org.tavall.web.frontend.generated.TavallForgeCSS;
import org.tavall.web.frontend.html.HTML;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Direction C Focus Workspace with request-checked QA, Edit, and Thread forms. */
public final class QaStaffWorkspacePageHTML {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private QaStaffWorkspacePageHTML() {
    }

    public static HTMLDocument build(TaskBoardTaskView taskView, QaWorkspacePageContext context) {
        if (taskView == null || taskView.task() == null || context == null) {
            return renderEmpty();
        }
        TaskBoardTask task = taskView.task();
        String taskId = task.taskId().toString();
        String taskPath = QaRoutes.task(task.projectKey(), task.taskId());

        HTMLElement shell = HTML.div().classes(
                        QaCSS.QA_APP,
                        QaCSS.PUBLIC_TASK_BOARD_PAGE,
                        QaCSS.PUBLIC_BOARD_MAIN,
                        TavallForgeCSS.FORGE_REVEAL
                )
                .data(QaWorkspaceDomDataRef.VARIANT, "C")
                .data(QaWorkspaceDomDataRef.STATE_PREVIEW, context.statePreview());

        shell.child(toolbar(task, context));
        shell.child(taskHero(task, taskPath, context));
        if (taskView.designReview() != null) {
            String reviewUrl = QaRoutes.designReview(task.projectKey(), task.taskId());
            if (context.organizationId().isPresent()) reviewUrl += "?organizationId=" + context.organizationId().get();
            shell.child(HTML.section().classes(QaCSS.PUBLIC_BOARD_SHELL).child(HTML.a()
                    .attribute(HTMLAttribute.HREF, reviewUrl).text(taskView.designReview().decided()
                            ? "View selected design " + taskView.designReview().winnerKey() : "Compare designs and select winner")));
        }


        HTMLElement panels = HTML.main().classes(QaCSS.PUBLIC_BOARD_SHELL);
        panels.child(overviewPanel(taskView, context));
        panels.child(qaPanel(taskView, context));
        panels.child(editPanel(taskView, context));
        panels.child(threadPanel(taskView, context));
        shell.child(panels);
        return Forge.page(QaProduct.product(QaRoutes.ROOT), shell);
    }

    private static HTMLElement toolbar(TaskBoardTask task, QaWorkspacePageContext context) {
        HTMLElement toolbar = HTML.section().classes(
                QaCSS.PUBLIC_BOARD_STAFF_STRIP,
                QaCSS.PUBLIC_STAFF_WORKSPACE_TOOLBAR
        );
        HTMLElement toolShell = HTML.div().classes(QaCSS.PUBLIC_BOARD_SHELL);
        HTMLElement toolTop = HTML.div().classes(QaCSS.PUBLIC_STAFF_WORKSPACE_TOOLBAR_TOP);
        HTMLElement titleBox = HTML.div().classes(QaCSS.PUBLIC_STAFF_WORKSPACE_TITLE);
        titleBox.child(HTML.span().text("Staff task workspace"));
        titleBox.child(HTML.strong().text("Direction C · Focus Workspace"));
        toolTop.child(titleBox);

        HTMLElement navTabs = HTML.nav().classes(QaCSS.PUBLIC_STAFF_VARIANT_TABS)
                .attribute(HTMLAttribute.ARIA_LABEL, "Focus modes");
        for (String mode : List.of("OVERVIEW", "QA", "EDIT", "THREAD")) {
            boolean active = mode.equalsIgnoreCase(context.activeFocus());
            HTMLElement tab = HTML.button().classes(QaCSS.GHOST_BUTTON)
                    .attribute(HTMLAttribute.TYPE, "button")
                    .attribute(HTMLAttribute.ARIA_PRESSED, Boolean.toString(active))
                    .data(QaWorkspaceDomDataRef.FOCUS, mode)
                    .text(mode.charAt(0) + mode.substring(1).toLowerCase(Locale.ROOT));
            if (active) {
                tab.classes(QaCSS.ACTIVE);
            }
            navTabs.child(tab);
        }
        toolTop.child(navTabs);
        toolShell.child(toolTop);

        HTMLElement matrix = HTML.div().classes(QaCSS.PUBLIC_STAFF_STATE_MATRIX);
        matrix.child(matrixFact("Work", task.status().name()));
        matrix.child(matrixFact("QA", task.qaStatus().name()));
        matrix.child(matrixFact("Visibility", task.audience().name()));
        matrix.child(matrixFact("Identity", context.linkedMinecraftIdentity() ? "LINKED" : "UNLINKED"));
        matrix.child(matrixFact("Mode", context.activeFocus().toUpperCase(Locale.ROOT)));
        toolShell.child(matrix);
        toolbar.child(toolShell);
        return toolbar;
    }

    private static HTMLElement matrixFact(String label, String value) {
        return HTML.div().child(HTML.span().text(label)).child(HTML.strong().text(value));
    }

    private static HTMLElement taskHero(
            TaskBoardTask task,
            String taskPath,
            QaWorkspacePageContext context
    ) {
        HTMLElement hero = HTML.section().classes(QaCSS.PUBLIC_BOARD_TASK_HERO);
        HTMLElement shell = HTML.div().classes(QaCSS.PUBLIC_BOARD_SHELL);
        shell.child(HTML.a().classes(QaCSS.PUBLIC_BOARD_BACK)
                .attribute(HTMLAttribute.HREF, organizationHref(taskPath, context.organizationId()))
                .text("← Task detail"));
        shell.child(HTML.h1().text(task.title()));
        hero.child(shell);
        return hero;
    }

    private static HTMLElement overviewPanel(TaskBoardTaskView taskView, QaWorkspacePageContext context) {
        TaskBoardTask task = taskView.task();
        HTMLElement panel = panel("OVERVIEW", "Task overview");
        panel.child(HTML.p().text(task.description().isBlank()
                ? "No detailed description provided."
                : task.description()));
        panel.child(HTML.p().classes(QaCSS.PUBLIC_WEIGHT_NOTE)
                .text("Material requirement, audience, weighting, or source changes invalidate prior Human Verify and make older evidence stale."));
        panel.child(HTML.h3().text("Recent task history"));
        if (taskView.history().isEmpty()) {
            panel.child(HTML.p().classes(QaCSS.PUBLIC_BOARD_EMPTY).text("No task history has been recorded."));
        } else {
            for (TaskBoardAuditView view : taskView.history()) {
                TaskBoardAuditEntry event = view.event();
                panel.child(HTML.div().classes(QaCSS.PUBLIC_STAFF_VERIFICATION_FACTS)
                        .child(HTML.div().child(HTML.strong().text(event.eventType().name()))
                                .child(HTML.span().text(DATE_FORMATTER.format(event.occurredAt()))))
                        .child(HTML.p().text(event.summary())));
            }
        }
        return panel;
    }

    private static HTMLElement qaPanel(TaskBoardTaskView taskView, QaWorkspacePageContext context) {
        TaskBoardTask task = taskView.task();
        String basePath = QaRoutes.task(task.projectKey(), task.taskId());
        HTMLElement panel = panel("QA", "Evidence, review, and Human Verify");
        panel.child(HTML.p().classes(QaCSS.PUBLIC_WEIGHT_NOTE)
                .text("DONE remains NHV until an authorized verifier records accepted evidence and a linked Minecraft identity."));

        panel.child(HTML.h3().text("Evidence submissions"));
        if (taskView.evidence().isEmpty()) {
            panel.child(HTML.p().classes(QaCSS.PUBLIC_BOARD_EMPTY).text("No evidence has been submitted."));
        }
        for (TaskBoardEvidenceView evidenceView : taskView.evidence()) {
            panel.child(evidenceEntry(task, evidenceView.evidence(), context, basePath));
        }
        if (!context.evidenceEnvironments().isEmpty()) {
            panel.child(evidenceSubmissionForm(task, context, basePath));
        }
        if (!context.acceptanceEnvironments().isEmpty()) {
            if (task.status() == TaskBoardTaskStatus.REVIEW || task.status() == TaskBoardTaskStatus.DONE) {
                panel.child(taskAcceptanceForms(task, taskView.evidence().stream()
                        .map(TaskBoardEvidenceView::evidence).toList(), context, basePath));
                panel.child(taskRejectionForm(context, basePath));
            }
        }

        boolean hasVerifiableEvidence = taskView.evidence().stream()
                .map(TaskBoardEvidenceView::evidence)
                .anyMatch(evidence -> context.verifiableEvidenceIds().contains(evidence.evidenceId())
                        && evidence.status() == TaskBoardEvidenceStatus.ACCEPTED
                        && evidence.currentFor(TaskBoardTaskFingerprint.compute(task)));
        if (hasVerifiableEvidence && task.status() == TaskBoardTaskStatus.DONE) {
            panel.child(humanVerifyForms(task, taskView.evidence(), context, basePath));
        }
        if (context.canAssignTask()) {
            panel.child(assignmentForm(task, context, basePath));
        }
        if (!taskView.assignments().isEmpty()) {
            panel.child(assignmentList(taskView.assignments(), task, context, basePath));
        }
        return panel;
    }

    private static HTMLElement evidenceEntry(
            TaskBoardTask task,
            TaskBoardEvidence evidence,
            QaWorkspacePageContext context,
            String taskPath
    ) {
        HTMLElement entry = HTML.article().classes(QaCSS.PUBLIC_SECTION_CARD);
        entry.child(HTML.h4().text(evidence.testCase()));
        entry.child(HTML.p().text(evidence.status().name() + " · " + evidence.environment().name()
                + " · " + DATE_FORMATTER.format(evidence.submittedAt())));
        entry.child(HTML.p().text("Build: " + evidence.buildReference()));
        entry.child(HTML.p().text("Expected: " + evidence.expectedResult()));
        entry.child(HTML.p().text("Observed: " + evidence.actualResult()));
        entry.child(HTML.a().attribute(HTMLAttribute.HREF, evidence.evidenceReference())
                .text("Open submitted evidence"));
        if (!evidence.reviewReason().isBlank()) {
            entry.child(HTML.p().classes(QaCSS.PUBLIC_WEIGHT_NOTE).text("Review: " + evidence.reviewReason()));
        }
        if (evidence.status() == TaskBoardEvidenceStatus.PENDING_REVIEW
                && context.reviewEnvironments().contains(evidence.environment())) {
            entry.child(evidenceReviewForm(evidence, context, taskPath));
        }
        if (context.verifiableEvidenceIds().contains(evidence.evidenceId())
                && evidence.status() == TaskBoardEvidenceStatus.ACCEPTED
                && evidence.currentFor(TaskBoardTaskFingerprint.compute(task))) {
            entry.child(HTML.p().classes(QaCSS.PUBLIC_WEIGHT_NOTE).text("This accepted record is eligible for Human Verify."));
        }
        return entry;
    }

    private static HTMLElement evidenceSubmissionForm(
            TaskBoardTask task,
            QaWorkspacePageContext context,
            String taskPath
    ) {
        HTMLElement form = form(taskPath + "/evidence", context);
        form.child(HTML.h3().text("Submit evidence"));
        form.child(environmentField(context.evidenceEnvironments(), "environment"));
        form.child(textField("Build / version tested", "buildReference", "", true));
        form.child(textField("Test case", "testCase", "", true));
        form.child(textArea("Expected result", "expectedResult", ""));
        form.child(textArea("Actual result", "actualResult", ""));
        form.child(textField("Evidence link (HTTPS)", "evidenceReference", "", true));
        form.child(textArea("Public-safe summary for review", "publicSummary", ""));
        form.child(submitButton("Submit evidence"));
        return HTML.div().classes(QaCSS.PUBLIC_STAFF_EDITOR).child(form);
    }

    private static HTMLElement taskRejectionForm(QaWorkspacePageContext context, String taskPath) {
        HTMLElement form = form(taskPath + "/reject", context);
        form.child(HTML.h3().text("Reject task acceptance"));
        form.child(environmentField(context.acceptanceEnvironments(), "environment"));
        form.child(textArea("Rejection reason", "reason", ""));
        form.child(submitButton("Return task to review"));
        return HTML.div().classes(QaCSS.PUBLIC_STAFF_EDITOR).child(form);
    }

    private static HTMLElement taskAcceptanceForms(
            TaskBoardTask task,
            List<TaskBoardEvidence> evidence,
            QaWorkspacePageContext context,
            String taskPath
    ) {
        HTMLElement group = HTML.div().classes(QaCSS.PUBLIC_STAFF_EDITOR);
        group.child(HTML.h3().text("Accept task"));
        group.child(HTML.p().text("Acceptance marks the task DONE and keeps it NHV until a separate Human Verify action."));
        String fingerprint = TaskBoardTaskFingerprint.compute(task);
        List<TaskBoardEvidence> acceptanceEvidence = evidence.stream()
                .filter(item -> item.status() == TaskBoardEvidenceStatus.ACCEPTED)
                .filter(item -> item.currentFor(fingerprint))
                .filter(item -> context.acceptanceEnvironments().contains(item.environment()))
                .sorted(Comparator.comparing(TaskBoardEvidence::submittedAt).reversed())
                .toList();
        if (acceptanceEvidence.isEmpty()) {
            group.child(HTML.p().classes(QaCSS.PUBLIC_BOARD_EMPTY).text("No accepted current evidence is ready for task acceptance."));
        } else {
            acceptanceEvidence.forEach(item -> {
                HTMLElement form = form(taskPath + "/accept", context);
                form.child(hidden("evidenceId", item.evidenceId().toString()));
                form.child(hidden("environment", item.environment().name()));
                form.child(HTML.p().text(item.testCase() + " · " + item.environment().name()));
                form.child(submitButton("Accept task with this evidence"));
                group.child(form);
            });
        }
        return group;
    }

    private static HTMLElement evidenceReviewForm(
            TaskBoardEvidence evidence,
            QaWorkspacePageContext context,
            String taskPath
    ) {
        HTMLElement form = form(taskPath + "/evidence/" + evidence.evidenceId() + "/review", context);
        form.child(HTML.h4().text("Review submission"));
        form.child(hidden("environment", evidence.environment().name()));
        form.child(selectField("Decision", "result", List.of("ACCEPTED", "REJECTED"), "ACCEPTED"));
        form.child(HTML.label()
                .child(HTML.input().attribute(HTMLAttribute.TYPE, "checkbox")
                        .attribute(HTMLAttribute.NAME, "publicSummaryApproved")
                        .attribute(HTMLAttribute.VALUE, "true"))
                .child(HTML.span().text("Approve the public-safe summary")));
        form.child(textArea("Review reason", "reason", ""));
        form.child(submitButton("Record review"));
        return form;
    }

    private static HTMLElement humanVerifyForms(
            TaskBoardTask task,
            List<TaskBoardEvidenceView> evidenceViews,
            QaWorkspacePageContext context,
            String taskPath
    ) {
        HTMLElement group = HTML.div().classes(QaCSS.PUBLIC_STAFF_EDITOR);
        group.child(HTML.h3().text("Human Verify"));
        group.child(HTML.p().text("The backend will recheck current authorization, environment scope, evidence, and linked Minecraft identity."));
        for (TaskBoardEvidenceView evidenceView : evidenceViews) {
            TaskBoardEvidence evidence = evidenceView.evidence();
            if (!context.verifiableEvidenceIds().contains(evidence.evidenceId())
                    || evidence.status() != TaskBoardEvidenceStatus.ACCEPTED
                    || !evidence.currentFor(TaskBoardTaskFingerprint.compute(task))) {
                continue;
            }
            HTMLElement verify = form(taskPath + "/verify", context);
            verify.child(hidden("evidenceId", evidence.evidenceId().toString()));
            verify.child(hidden("environment", evidence.environment().name()));
            verify.child(HTML.p().text(evidence.testCase() + " · " + evidence.environment().name()));
            verify.child(submitButton("Human Verify this evidence"));
            group.child(verify);
        }
        return group;
    }

    private static HTMLElement assignmentForm(
            TaskBoardTask task,
            QaWorkspacePageContext context,
            String taskPath
    ) {
        HTMLElement form = form(taskPath + "/assignments", context);
        form.child(HTML.h3().text("Assign scoped QA work"));
        form.child(textField("Tavall Account ID", "accountId", "", true));
        form.child(selectField("Capability", "capability", List.of(
                QaCapability.VIEW_QA_TASKS.name(),
                QaCapability.SUBMIT_EVIDENCE.name(),
                QaCapability.REVIEW_EVIDENCE.name(),
                QaCapability.ADD_COMMENT.name(),
                QaCapability.HUMAN_VERIFY.name()
        ), QaCapability.SUBMIT_EVIDENCE.name()));
        form.child(environmentField(Set.of(QaEnvironmentGrant.DEV, QaEnvironmentGrant.STAGING, QaEnvironmentGrant.PROD), "environment"));
        form.child(selectField("Required QA class", "requiredClass", List.of("", QaClass.REGULAR.name(), QaClass.TECHNICAL.name(), QaClass.FOUNDING_PUBLIC.name()), ""));
        form.child(selectField("Minimum QA tier", "minimumTier", List.of("", "TIER_0", "TIER_I", "TIER_II", "TIER_III", "TIER_IV", "TIER_V"), ""));
        form.child(textArea("Reason", "reason", ""));
        form.child(submitButton("Assign capability"));
        return HTML.div().classes(QaCSS.PUBLIC_STAFF_EDITOR).child(form);
    }

    private static HTMLElement assignmentList(
            List<TaskBoardAssignmentView> assignmentViews,
            TaskBoardTask task,
            QaWorkspacePageContext context,
            String taskPath
    ) {
        HTMLElement group = HTML.div().classes(QaCSS.PUBLIC_STAFF_EDITOR);
        group.child(HTML.h3().text("Task assignments"));
        for (TaskBoardAssignmentView assignmentView : assignmentViews) {
            TaskBoardAssignment assignment = assignmentView.assignment();
            HTMLElement row = HTML.div().classes(QaCSS.PUBLIC_STAFF_VERIFICATION_FACTS);
            row.child(HTML.div()
                    .child(HTML.strong().text(assignment.capability().name()))
                    .child(HTML.span().text(assignment.environment().name())));
            row.child(HTML.p().text("Account " + assignment.accountId() + " · " + assignment.reason()));
            if (assignment.revokedAt() == null && context.canAssignTask()) {
                HTMLElement revoke = form(taskPath + "/assignments/" + assignment.assignmentId() + "/revoke", context);
                revoke.child(textField("Revocation reason", "reason", "", true));
                revoke.child(submitButton("Revoke assignment"));
                row.child(revoke);
            }
            group.child(row);
        }
        return group;
    }

    private static HTMLElement editPanel(TaskBoardTaskView taskView, QaWorkspacePageContext context) {
        TaskBoardTask task = taskView.task();
        String taskPath = QaRoutes.task(task.projectKey(), task.taskId());
        HTMLElement panel = panel("EDIT", "Edit task and destinations");
        if (!context.canManageTask()) {
            panel.child(HTML.p().classes(QaCSS.PUBLIC_BOARD_EMPTY).text("Task editing is not granted for this Account."));
            return panel;
        }

        panel.child(HTML.p().classes(QaCSS.PUBLIC_WEIGHT_NOTE)
                .text("Material changes to requirements, audience, weighting, or source invalidate prior Human Verify and stale earlier evidence."));
        HTMLElement form = form(taskPath, context);
        form.child(textField("Section", "sectionKey", task.sectionKey(), true));
        form.child(textField("Title", "title", task.title(), true));
        form.child(textField("Public title", "publicTitle", task.publicTitle(), false));
        form.child(textArea("Description", "description", task.description()));
        form.child(textArea("Public summary", "publicSummary", task.publicSummary()));
        form.child(selectField("Work status", "status", enumNames(TaskBoardTaskStatus.values()), task.status().name()));
        form.child(selectField("Priority", "priority", enumNames(TaskBoardTaskPriority.values()), task.priority().name()));
        form.child(selectField("Audience", "audience", enumNames(org.tavall.qa.domain.TaskBoardAudience.values()), task.audience().name()));
        form.child(textField("Weight", "weight", Integer.toString(task.weight()), true));
        form.child(selectField("Hosted validation required", "hostValidationRequired", List.of("false", "true"), Boolean.toString(task.hostValidationRequired())));
        form.child(selectField("Counts for VQC", "qaQcEligible", List.of("false", "true"), Boolean.toString(task.qaQcEligible())));
        form.child(textField("Source type", "sourceType", task.sourceType(), false));
        form.child(textField("Source reference", "sourceReference", task.sourceReference(), false));
        form.child(textField("Sort order", "sortOrder", Integer.toString(task.sortOrder()), true));
        form.child(submitButton("Save task"));
        panel.child(form);

        HTMLElement actionForm = form(taskPath + "/actions", context);
        actionForm.child(HTML.h3().text("Add destination"));
        actionForm.child(selectField("Action type", "type", enumNames(TaskBoardActionType.values()), TaskBoardActionType.REPOSITORY_FILE.name()));
        actionForm.child(textField("Label", "label", "", true));
        actionForm.child(textField("Target (approved URL or source path)", "target", "", true));
        actionForm.child(textField("Sort order", "sortOrder", "0", true));
        actionForm.child(submitButton("Add destination"));
        panel.child(HTML.div().classes(QaCSS.PUBLIC_STAFF_EDITOR).child(actionForm));
        return panel;
    }

    private static HTMLElement threadPanel(TaskBoardTaskView taskView, QaWorkspacePageContext context) {
        TaskBoardTask task = taskView.task();
        String taskPath = QaRoutes.task(task.projectKey(), task.taskId());
        HTMLElement panel = panel("THREAD", "Comments, suggestions, and QA notes");
        if (taskView.comments().isEmpty()) {
            panel.child(HTML.p().classes(QaCSS.PUBLIC_BOARD_EMPTY).text("No thread entries have been recorded."));
        } else {
            for (TaskBoardComment comment : taskView.comments()) {
                panel.child(HTML.article().classes(QaCSS.PUBLIC_SECTION_CARD)
                        .child(HTML.h3().text(comment.type().name()))
                        .child(HTML.p().text(DATE_FORMATTER.format(comment.createdAt())))
                        .child(HTML.p().text(comment.body())));
            }
        }

        Set<QaEnvironmentGrant> commentEnvironments = context.commentEnvironments();
        Set<QaEnvironmentGrant> noteEnvironments = context.reviewEnvironments();
        if (!commentEnvironments.isEmpty() || !noteEnvironments.isEmpty()) {
            List<String> types = new ArrayList<>();
            if (!commentEnvironments.isEmpty()) {
                types.add(TaskBoardCommentType.COMMENT.name());
                types.add(TaskBoardCommentType.SUGGESTION.name());
            }
            if (!noteEnvironments.isEmpty()) {
                types.add(TaskBoardCommentType.QA_NOTE.name());
            }
            Set<QaEnvironmentGrant> environments = new java.util.LinkedHashSet<>(commentEnvironments);
            environments.addAll(noteEnvironments);
            HTMLElement form = form(taskPath + "/comments", context);
            form.child(selectField("Entry type", "type", types, types.getFirst()));
            form.child(environmentField(environments, "environment"));
            form.child(textArea("Comment or QA note", "body", ""));
            form.child(submitButton("Post entry"));
            panel.child(form);
        }
        return panel;
    }

    private static HTMLElement panel(String focus, String title) {
        HTMLElement panel = HTML.section().classes(QaCSS.PUBLIC_STAFF_WORKSPACE_PANEL)
                .data(QaWorkspaceDomDataRef.PANEL, focus);
        panel.child(HTML.h2().text(title));
        return panel;
    }

    private static HTMLElement form(String action, QaWorkspacePageContext context) {
        HTMLElement form = HTML.form().classes(QaCSS.PUBLIC_STAFF_FORM)
                .attribute(HTMLAttribute.METHOD, "post")
                .attribute(HTMLAttribute.ACTION, action);
        if (!context.csrfToken().isBlank()) {
            form.child(hidden(context.csrfParameterName(), context.csrfToken()));
        }
        context.organizationId().ifPresent(id -> form.child(hidden("organizationId", id.toString())));
        return form;
    }

    private static HTMLElement textField(String label, String name, String value, boolean required) {
        HTMLElement input = HTML.input().attribute(HTMLAttribute.TYPE, "text")
                .attribute(HTMLAttribute.NAME, name)
                .attribute(HTMLAttribute.VALUE, value);
        if (required) {
            input.attribute(HTMLAttribute.REQUIRED, "required");
        }
        return HTML.label().child(HTML.span().text(label)).child(input);
    }

    private static HTMLElement textArea(String label, String name, String value) {
        return HTML.label().classes(QaCSS.WIDE)
                .child(HTML.span().text(label))
                .child(HTML.textarea().attribute(HTMLAttribute.NAME, name).text(value));
    }

    private static HTMLElement selectField(String label, String name, List<String> values, String selectedValue) {
        HTMLElement select = HTML.select().attribute(HTMLAttribute.NAME, name);
        for (String value : values) {
            HTMLElement option = HTML.option().attribute(HTMLAttribute.VALUE, value).text(value.isBlank() ? "Not required" : value);
            if (value.equals(selectedValue)) {
                option.attribute(HTMLAttribute.SELECTED, "selected");
            }
            select.child(option);
        }
        return HTML.label().child(HTML.span().text(label)).child(select);
    }

    private static HTMLElement environmentField(Set<QaEnvironmentGrant> environments, String name) {
        return selectField("Environment", name,
                environments.stream().sorted().map(Enum::name).toList(),
                environments.stream().sorted().findFirst().map(Enum::name).orElse(""));
    }

    private static List<String> enumNames(Enum<?>[] values) {
        return java.util.Arrays.stream(values).map(Enum::name).toList();
    }

    private static HTMLElement hidden(String name, String value) {
        return HTML.input().attribute(HTMLAttribute.TYPE, "hidden")
                .attribute(HTMLAttribute.NAME, name)
                .attribute(HTMLAttribute.VALUE, value);
    }

    private static HTMLElement submitButton(String label) {
        return HTML.div().classes(QaCSS.PUBLIC_STAFF_FORM_ACTIONS)
                .child(HTML.button().classes(QaCSS.GOLD_BUTTON)
                        .attribute(HTMLAttribute.TYPE, "submit")
                        .text(label));
    }

    private static String organizationHref(String path, Optional<UUID> organizationId) {
        return organizationId.map(id -> path + "?organizationId=" + id).orElse(path);
    }

    private static HTMLDocument renderEmpty() {
        HTMLElement shell = HTML.div().classes(
                QaCSS.QA_APP,
                QaCSS.PUBLIC_TASK_BOARD_PAGE,
                QaCSS.PUBLIC_BOARD_MAIN,
                TavallForgeCSS.FORGE_REVEAL
        );
        shell.child(HTML.div().classes(QaCSS.PUBLIC_BOARD_SHELL, QaCSS.PUBLIC_BOARD_NOTICE)
                .text("Task not found or unavailable."));
        return Forge.page(QaProduct.product(QaRoutes.ROOT), shell);
    }
}
