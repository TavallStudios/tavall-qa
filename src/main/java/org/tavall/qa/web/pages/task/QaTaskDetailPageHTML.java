package org.tavall.qa.web.pages.task;

import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardEvidenceStatus;
import org.tavall.qa.domain.TaskBoardTaskFingerprint;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.view.TaskBoardAssignedEvidenceView;
import org.tavall.qa.view.TaskBoardEvidenceView;
import org.tavall.qa.view.TaskBoardPublicEvidenceView;
import org.tavall.qa.view.TaskBoardResolvedActionView;
import org.tavall.qa.view.TaskBoardTaskPageView;
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

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Comparator;
import java.util.UUID;

/**
 * Direction A Task Detail layout:
 * Evidence-first structured summary, acceptance criteria, human verification audit trail,
 * and contextual action rail.
 */
public final class QaTaskDetailPageHTML {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private QaTaskDetailPageHTML() {
    }

    public static HTMLDocument build(
            TaskBoardTaskView taskView,
            boolean canManage,
            boolean canHumanVerify
    ) {
        return build(taskView, canManage, canHumanVerify, QaTaskPageContext.readOnly());
    }

    public static HTMLDocument build(
            TaskBoardTaskView taskView,
            boolean canManage,
            boolean canHumanVerify,
            QaTaskPageContext pageContext
    ) {
        QaTaskExecutionView execution = pageContext != null ? pageContext.execution().orElse(null) : null;
        return build(taskView, canManage, canHumanVerify, pageContext, execution);
    }

    public static HTMLDocument build(
            TaskBoardTaskView taskView,
            boolean canManage,
            boolean canHumanVerify,
            QaTaskPageContext pageContext,
            QaTaskExecutionView execution
    ) {
        if (taskView == null || taskView.task() == null) {
            return renderEmpty();
        }
        TaskBoardTask task = taskView.task();
        String projectKey = task.projectKey();
        String projectDisplayName = taskView.project() != null ? taskView.project().displayName() : projectKey;
        return renderTask(
                projectKey,
                projectDisplayName,
                task.sectionKey(),
                task.taskId().toString(),
                task.title(),
                task.publicSummary(),
                task.description(),
                task.status().name(),
                task.priority().name(),
                task.qaStatus(),
                task.audience().name(),
                taskView.project() != null && taskView.project().publicVisible() && task.publicVisible(),
                task.weight(),
                task.humanVerified(),
                task.verifiedByMinecraftUUID() != null ? task.verifiedByMinecraftUUID().toString() : null,
                task.humanVerifiedAt(),
                task.hostValidationRequired(),
                taskView.actions(),
                taskView.evidence().stream()
                        .map(TaskBoardEvidenceView::evidence)
                        .map(evidence -> new TaskBoardAssignedEvidenceView(
                                evidence.evidenceId(), evidence.environment(), evidence.buildReference(),
                                evidence.testCase(), evidence.expectedResult(), evidence.actualResult(),
                                evidence.evidenceReference(), evidence.publicSummary(), evidence.status(),
                                evidence.currentFor(TaskBoardTaskFingerprint.compute(task)), evidence.submittedAt()))
                        .toList(),
                taskView.evidence().stream()
                        .map(TaskBoardEvidenceView::evidence)
                        .filter(evidence -> evidence.status() == TaskBoardEvidenceStatus.ACCEPTED)
                        .filter(evidence -> evidence.publicSummaryApproved())
                        .filter(evidence -> evidence.currentFor(TaskBoardTaskFingerprint.compute(task)))
                        .map(evidence -> new TaskBoardPublicEvidenceView(
                                evidence.publicSummary(), evidence.environment(), evidence.submittedAt()))
                        .toList(),
                canManage,
                canHumanVerify,
                pageContext,
                execution
        );
    }

    public static HTMLDocument build(
            TaskBoardTaskPageView taskPage,
            boolean canManage,
            boolean canHumanVerify
    ) {
        return build(taskPage, canManage, canHumanVerify, QaTaskPageContext.readOnly(), null);
    }

    public static HTMLDocument build(
            TaskBoardTaskPageView taskPage,
            boolean canManage,
            boolean canHumanVerify,
            QaTaskPageContext pageContext
    ) {
        QaTaskExecutionView execution = pageContext != null ? pageContext.execution().orElse(null) : null;
        return build(taskPage, canManage, canHumanVerify, pageContext, execution);
    }

    public static HTMLDocument build(
            TaskBoardTaskPageView taskPage,
            boolean canManage,
            boolean canHumanVerify,
            QaTaskPageContext pageContext,
            QaTaskExecutionView execution
    ) {
        if (taskPage == null) {
            return renderEmpty();
        }
        return renderTask(
                taskPage.projectKey(),
                taskPage.projectDisplayName(),
                taskPage.sectionKey(),
                taskPage.taskId().toString(),
                taskPage.title(),
                taskPage.summary(),
                taskPage.summary(),
                taskPage.status().name(),
                taskPage.priority().name(),
                taskPage.qaStatus(),
                taskPage.audience().name(),
                taskPage.publicVisible(),
                taskPage.weight(),
                taskPage.qaStatus() == TaskBoardQaStatus.HUMAN_VERIFIED,
                null,
                null,
                false,
                List.of(),
                taskPage.assignedEvidence(),
                taskPage.publicEvidence(),
                canManage,
                canHumanVerify,
                pageContext,
                execution
        );
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

    private static HTMLDocument renderTask(
            String projectKey,
            String projectDisplayName,
            String sectionKey,
            String taskIdStr,
            String title,
            String publicSummary,
            String description,
            String statusName,
            String priorityName,
            TaskBoardQaStatus qaStatus,
            String audienceName,
            boolean publicVisible,
            int weight,
            boolean isHumanVerified,
            String verifierUuid,
            java.time.Instant humanVerifiedAt,
            boolean hostValidationRequired,
            List<TaskBoardResolvedActionView> actions,
            List<TaskBoardAssignedEvidenceView> assignedEvidence,
            List<TaskBoardPublicEvidenceView> evidence,
            boolean canManage,
            boolean canHumanVerify,
            QaTaskPageContext pageContext,
            QaTaskExecutionView execution
    ) {
        HTMLElement shell = HTML.div().classes(
                QaCSS.QA_APP,
                QaCSS.PUBLIC_TASK_BOARD_PAGE,
                QaCSS.PUBLIC_BOARD_MAIN,
                TavallForgeCSS.FORGE_REVEAL
        );

        // 1. Hero Section (Direction A)
        HTMLElement hero = HTML.section().classes(QaCSS.PUBLIC_BOARD_TASK_HERO);
        HTMLElement heroShell = HTML.div().classes(QaCSS.PUBLIC_BOARD_SHELL);

        String backPath = publicVisible ? QaRoutes.project(projectKey) : QaRoutes.ROOT;
        heroShell.child(HTML.a().classes(QaCSS.PUBLIC_BOARD_BACK)
                .attribute(HTMLAttribute.HREF, organizationHref(backPath, pageContext))
                .text(publicVisible ? "← " + projectDisplayName + " board" : "← QA board"));

        HTMLElement heroGrid = HTML.div().classes(QaCSS.PUBLIC_TASK_HERO_GRID);
        HTMLElement copy = HTML.div();
        copy.child(HTML.p().classes(QaCSS.PUBLIC_TASK_COPY)
                .text(sectionKey.replace('-', ' ').toUpperCase() + " · " + statusName));
        copy.child(HTML.h1().text(title));
        if (publicSummary != null && !publicSummary.isBlank()) {
            copy.child(HTML.p().classes(QaCSS.PUBLIC_BOARD_LEAD).text(publicSummary));
        }
        heroGrid.child(copy);

        // Facts dl
        HTMLElement facts = HTML.dl().classes(QaCSS.PUBLIC_TASK_FACTS);
        facts.child(HTML.div()
                .child(HTML.dt().text("QA"))
                .child(HTML.dd().text(qaStatus.name())));
        facts.child(HTML.div()
                .child(HTML.dt().text("Priority"))
                .child(HTML.dd().text(priorityName)));
        facts.child(HTML.div()
                .child(HTML.dt().text("Weight"))
                .child(HTML.dd().text(String.valueOf(weight))));
        facts.child(HTML.div()
                .child(HTML.dt().text("Visibility"))
                .child(HTML.dd().text(audienceName)));
        heroGrid.child(facts);
        heroShell.child(heroGrid);

        hero.child(heroShell);
        shell.child(hero);

        // 2. Staff workspace link if manager
        if (canManage) {
            HTMLElement staffStrip = HTML.section().classes(QaCSS.PUBLIC_BOARD_STAFF_STRIP);
            HTMLElement staffInner = HTML.div().classes(QaCSS.PUBLIC_BOARD_SHELL, QaCSS.PUBLIC_BOARD_STAFF_STRIP_INNER);
            staffInner.child(HTML.div()
                    .child(HTML.span().text("Staff workspace"))
                    .child(HTML.strong().text("Focus workspace active on this task.")));
            staffInner.child(HTML.a()
                    .attribute(HTMLAttribute.HREF, workspaceHref(projectKey, taskIdStr, pageContext))
                    .text("Open Focus Workspace →"));
            staffStrip.child(staffInner);
            shell.child(staffStrip);
        }

        // 3. Evidence Grid (Direction A: Main evidence + Action rail)
        HTMLElement bodySection = HTML.section().classes(QaCSS.PUBLIC_BOARD_SHELL, QaCSS.PUBLIC_TASK_DETAIL_LAYOUT);

        // Left / Main column: Structured summary, Acceptance criteria, Human verification audit
        HTMLElement mainCol = HTML.div().classes(QaCSS.PUBLIC_TASK_EVIDENCE);

        // Description
        HTMLElement descCard = HTML.article().classes(QaCSS.PUBLIC_SECTION_CARD);
        descCard.child(HTML.h3().text("Description"));
        descCard.child(HTML.p().text(description != null ? description : "No detailed description provided."));
        mainCol.child(descCard);

        // Acceptance Criteria
        HTMLElement criteriaCard = HTML.article().classes(QaCSS.PUBLIC_SECTION_CARD);
        criteriaCard.child(HTML.h3().text("Acceptance Criteria"));
        if (hostValidationRequired) {
            criteriaCard.child(HTML.p().classes(QaCSS.PUBLIC_WEIGHT_NOTE)
                    .text("Hosted/manual validation is strictly required for completion."));
        } else {
            criteriaCard.child(HTML.p().text("Standard module invariants and verification apply."));
        }
        mainCol.child(criteriaCard);

        HTMLElement evidenceCard = HTML.article().classes(QaCSS.PUBLIC_SECTION_CARD);
        evidenceCard.child(HTML.h3().text("Accepted QA evidence"));
        if (evidence == null || evidence.isEmpty()) {
            evidenceCard.child(HTML.p().classes(QaCSS.PUBLIC_BOARD_EMPTY)
                    .text("No public-safe QA evidence has been approved for this task."));
        } else {
            for (TaskBoardPublicEvidenceView item : evidence.stream()
                    .sorted(Comparator.comparing(TaskBoardPublicEvidenceView::submittedAt).reversed())
                    .toList()) {
                evidenceCard.child(HTML.div().classes(QaCSS.PUBLIC_STAFF_VERIFICATION_FACTS)
                        .child(HTML.strong().text(item.summary()))
                        .child(HTML.p().text(item.environment().name() + " · " + DATE_FORMATTER.format(item.submittedAt()))));
            }
        }
        mainCol.child(evidenceCard);

        if (assignedEvidence != null && !assignedEvidence.isEmpty()) {
            mainCol.child(assignedEvidenceSection(projectKey, taskIdStr, statusName, assignedEvidence, pageContext));
        }

        boolean isContributor = pageContext != null && !pageContext.evidenceEnvironments().isEmpty();
        boolean isQaReviewer = (pageContext != null && !pageContext.reviewEnvironments().isEmpty()) || canHumanVerify;
        boolean isAdmin = canManage;
        boolean isAuthorizedForExecution = isContributor || isQaReviewer || isAdmin;

        // Public views must not show execution internals
        if (execution != null && isAuthorizedForExecution) {
            mainCol.child(executionSection(projectKey, taskIdStr, execution, isContributor, isQaReviewer, isAdmin, pageContext));
        }

        if (pageContext != null && !pageContext.evidenceEnvironments().isEmpty()) {
            mainCol.child(evidenceSubmissionForm(projectKey, taskIdStr, pageContext));
        }
        if (pageContext != null && !pageContext.commentEnvironments().isEmpty()) {
            mainCol.child(suggestionForm(projectKey, taskIdStr, pageContext));
        }

        // Human Verification Audit Section (Direction A + NHV Policy)
        HTMLElement hvCard = HTML.article().classes(QaCSS.PUBLIC_STAFF_VERIFICATION_CARD);
        hvCard.child(HTML.h3().text("Human Verification Audit"));

        if (qaStatus == TaskBoardQaStatus.HUMAN_VERIFIED && isHumanVerified) {
            HTMLElement verifiedBox = HTML.div().classes(QaCSS.PUBLIC_STAFF_VERIFICATION_FACTS);
            verifiedBox.child(HTML.div()
                    .child(HTML.strong().text("HUMAN VERIFIED"))
                    .child(HTML.span().text("Verified on an active QA environment grant")));
            if (verifierUuid != null) {
                verifiedBox.child(HTML.p().text("Verifier Minecraft UUID: " + verifierUuid));
            }
            if (humanVerifiedAt != null) {
                verifiedBox.child(HTML.p().text("Verified at: " + DATE_FORMATTER.format(humanVerifiedAt)));
            }
            hvCard.child(verifiedBox);
        } else {
            // NHV Default State
            HTMLElement nhvBox = HTML.div().classes(QaCSS.PUBLIC_STAFF_PREVIEW_NOTE);
            nhvBox.child(HTML.strong().text("NHV — Not Human Verified"));
            nhvBox.child(HTML.p().text("This task is pending human QA verification on an active environment grant by an authorized verifier with a linked Minecraft identity."));
            hvCard.child(nhvBox);
        }
        mainCol.child(hvCard);
        bodySection.child(mainCol);

        // Right column: Direction A Action Rail
        HTMLElement railCol = HTML.aside().classes(QaCSS.PUBLIC_TASK_CONTEXT_RAIL);
        railCol.child(HTML.h3().text("Actions & Destinations"));

        if (actions != null && !actions.isEmpty()) {
            HTMLElement actionList = HTML.div().classes(QaCSS.PUBLIC_TASK_ACTION_LIST);
            for (TaskBoardResolvedActionView action : actions) {
                HTMLElement actionItem = HTML.a().classes(QaCSS.PUBLIC_TASK_ACTION)
                        .attribute(HTMLAttribute.HREF, action.href())
                        .attribute(HTMLAttribute.ROLE, "button");
                actionItem.child(HTML.span().text(action.action().type().name()));
                actionItem.child(HTML.strong().text(action.action().label()));
                actionList.child(actionItem);
            }
            railCol.child(actionList);
        } else {
            railCol.child(HTML.p().classes(QaCSS.PUBLIC_BOARD_EMPTY).text("No external destinations linked."));
        }

        bodySection.child(railCol);
        shell.child(bodySection);

        return Forge.page(QaProduct.product(QaRoutes.ROOT), shell);
    }

    private static HTMLElement assignedEvidenceSection(
            String projectKey,
            String taskId,
            String taskStatus,
            List<TaskBoardAssignedEvidenceView> evidence,
            QaTaskPageContext context
    ) {
        HTMLElement section = HTML.article().classes(QaCSS.PUBLIC_SECTION_CARD);
        section.child(HTML.h3().text("Evidence for this assignment"));
        for (TaskBoardAssignedEvidenceView item : evidence.stream()
                .sorted(Comparator.comparing(TaskBoardAssignedEvidenceView::submittedAt).reversed())
                .toList()) {
            HTMLElement entry = HTML.div().classes(QaCSS.PUBLIC_STAFF_VERIFICATION_FACTS);
            entry.child(HTML.div()
                    .child(HTML.strong().text(item.testCase()))
                    .child(HTML.span().text(item.status().name() + " · " + item.environment().name())));
            entry.child(HTML.p().text("Build: " + item.buildReference()));
            entry.child(HTML.p().text("Expected: " + item.expectedResult()));
            entry.child(HTML.p().text("Observed: " + item.actualResult()));
            entry.child(HTML.a().attribute(HTMLAttribute.HREF, item.evidenceReference()).text("Open submitted evidence"));
            entry.child(HTML.p().text(item.currentForTask() ? "Current task version" : "Stale: retest after the task changed"));
            if (item.status() == TaskBoardEvidenceStatus.PENDING_REVIEW
                    && context != null
                    && context.reviewEnvironments().contains(item.environment())) {
                entry.child(evidenceReviewForm(projectKey, taskId, item, context));
            }
            if (item.status() == TaskBoardEvidenceStatus.ACCEPTED
                    && item.currentForTask()
                    && context != null
                    && context.verifiableEvidenceIds().contains(item.evidenceId())) {
                entry.child(humanVerifyForm(projectKey, taskId, item, context));
            }
            if (("REVIEW".equals(taskStatus) || "DONE".equals(taskStatus))
                    && item.status() == TaskBoardEvidenceStatus.ACCEPTED
                    && item.currentForTask()
                    && context != null
                    && context.acceptanceEnvironments().contains(item.environment())) {
                entry.child(taskAcceptanceForm(projectKey, taskId, item, context));
            }
            section.child(entry);
        }
        return section;
    }

    private static HTMLElement taskAcceptanceForm(
            String projectKey,
            String taskId,
            TaskBoardAssignedEvidenceView evidence,
            QaTaskPageContext context
    ) {
        HTMLElement form = HTML.form().classes(QaCSS.PUBLIC_STAFF_FORM)
                .attribute(HTMLAttribute.METHOD, "post")
                .attribute(HTMLAttribute.ACTION, "/projects/" + projectKey + "/tasks/" + taskId + "/accept");
        addRequestContext(form, context);
        form.child(hidden("evidenceId", evidence.evidenceId().toString()));
        form.child(hidden("environment", evidence.environment().name()));
        form.child(submitButton("Accept task · remains NHV"));
        return form;
    }

    private static HTMLElement evidenceReviewForm(
            String projectKey,
            String taskId,
            TaskBoardAssignedEvidenceView evidence,
            QaTaskPageContext context
    ) {
        String action = "/projects/" + projectKey + "/tasks/" + taskId
                + "/evidence/" + evidence.evidenceId() + "/review";
        HTMLElement form = HTML.form().classes(QaCSS.PUBLIC_STAFF_FORM)
                .attribute(HTMLAttribute.METHOD, "post")
                .attribute(HTMLAttribute.ACTION, action);
        addRequestContext(form, context);
        form.child(hidden("environment", evidence.environment().name()));
        form.child(selectField("Decision", "result", List.of("ACCEPTED", "REJECTED"), "ACCEPTED"));
        form.child(HTML.label()
                .child(HTML.input().attribute(HTMLAttribute.TYPE, "checkbox")
                        .attribute(HTMLAttribute.NAME, "publicSummaryApproved")
                        .attribute(HTMLAttribute.VALUE, "true"))
                .child(HTML.span().text("Approve public-safe summary")));
        form.child(textArea("Review reason", "reason", ""));
        form.child(submitButton("Record review"));
        return form;
    }

    private static HTMLElement humanVerifyForm(
            String projectKey,
            String taskId,
            TaskBoardAssignedEvidenceView evidence,
            QaTaskPageContext context
    ) {
        HTMLElement form = HTML.form().classes(QaCSS.PUBLIC_STAFF_FORM)
                .attribute(HTMLAttribute.METHOD, "post")
                .attribute(HTMLAttribute.ACTION, "/projects/" + projectKey + "/tasks/" + taskId + "/verify");
        addRequestContext(form, context);
        form.child(hidden("evidenceId", evidence.evidenceId().toString()));
        form.child(hidden("environment", evidence.environment().name()));
        form.child(submitButton("Human Verify this evidence"));
        return form;
    }

    private static HTMLElement evidenceSubmissionForm(
            String projectKey,
            String taskId,
            QaTaskPageContext context
    ) {
        HTMLElement form = HTML.form().classes(QaCSS.PUBLIC_STAFF_FORM)
                .attribute(HTMLAttribute.METHOD, "post")
                .attribute(HTMLAttribute.ACTION, "/projects/" + projectKey + "/tasks/" + taskId + "/evidence");
        addRequestContext(form, context);
        form.child(HTML.h3().text("Submit QA evidence"));
        form.child(selectField("Environment", "environment", context.evidenceEnvironments().stream()
                .sorted().map(Enum::name).toList(), context.evidenceEnvironments().stream().sorted()
                .findFirst().map(Enum::name).orElse("")));
        form.child(textInput("Build / version tested", "buildReference", "", true));
        form.child(textInput("Test case", "testCase", "", true));
        form.child(textArea("Expected result", "expectedResult", ""));
        form.child(textArea("Actual result", "actualResult", ""));
        form.child(textInput("Evidence link (HTTPS)", "evidenceReference", "", true));
        form.child(textArea("Public-safe summary (optional)", "publicSummary", ""));
        form.child(HTML.button().classes(QaCSS.GOLD_BUTTON)
                .attribute(HTMLAttribute.TYPE, "submit")
                .text("Submit evidence"));
        return HTML.article().classes(QaCSS.PUBLIC_SECTION_CARD).child(form);
    }

    private static HTMLElement suggestionForm(
            String projectKey,
            String taskId,
            QaTaskPageContext context
    ) {
        HTMLElement form = HTML.form().classes(QaCSS.PUBLIC_STAFF_FORM)
                .attribute(HTMLAttribute.METHOD, "post")
                .attribute(HTMLAttribute.ACTION, "/projects/" + projectKey + "/tasks/" + taskId + "/comments");
        addRequestContext(form, context);
        form.child(HTML.h3().text("Suggest an improvement"));
        form.child(hidden("type", "SUGGESTION"));
        form.child(selectField("Environment", "environment", context.commentEnvironments().stream()
                .sorted().map(Enum::name).toList(), context.commentEnvironments().stream().sorted()
                .findFirst().map(Enum::name).orElse("")));
        form.child(textArea("Suggestion", "body", ""));
        form.child(HTML.button().classes(QaCSS.GOLD_BUTTON)
                .attribute(HTMLAttribute.TYPE, "submit")
                .text("Post suggestion"));
        return HTML.article().classes(QaCSS.PUBLIC_SECTION_CARD).child(form);
    }

    private static HTMLElement textInput(String label, String name, String value, boolean required) {
        HTMLElement input = HTML.input()
                .attribute(HTMLAttribute.TYPE, "text")
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
            HTMLElement option = HTML.option().attribute(HTMLAttribute.VALUE, value).text(value);
            if (value.equals(selectedValue)) {
                option.attribute(HTMLAttribute.SELECTED, "selected");
            }
            select.child(option);
        }
        return HTML.label().child(HTML.span().text(label)).child(select);
    }

    private static HTMLElement executionSection(
            String projectKey,
            String taskId,
            QaTaskExecutionView execution,
            boolean isContributor,
            boolean isQaReviewer,
            boolean isAdmin,
            QaTaskPageContext context
    ) {
        HTMLElement section = HTML.article().classes(QaCSS.PUBLIC_SECTION_CARD);
        section.child(HTML.h3().text("Execution & Diagnostics"));

        // Facts dl for execution metadata
        HTMLElement facts = HTML.dl().classes(QaCSS.PUBLIC_TASK_FACTS);

        // 1. Lease holder
        String leaseText = execution.leaseHolder() != null && !execution.leaseHolder().isBlank()
                ? execution.leaseHolder()
                : "None (Unleased)";
        facts.child(HTML.div()
                .child(HTML.dt().text("Lease Holder"))
                .child(HTML.dd().text(leaseText)));

        // 2. Provider ID
        String providerText = execution.providerId() != null && !execution.providerId().isBlank()
                ? execution.providerId()
                : "None";
        facts.child(HTML.div()
                .child(HTML.dt().text("Provider ID"))
                .child(HTML.dd().text(providerText)));

        // 3. Run ID
        String runText = execution.runId() != null
                ? execution.runId().toString()
                : "None";
        facts.child(HTML.div()
                .child(HTML.dt().text("Run ID"))
                .child(HTML.dd().text(runText)));

        // 4. Current quota restriction / blocked reason
        String quotaText = execution.quotaRestriction() != null && !execution.quotaRestriction().isBlank()
                ? execution.quotaRestriction()
                : "None";
        facts.child(HTML.div()
                .child(HTML.dt().text("Quota Restriction"))
                .child(HTML.dd().text(quotaText)));

        String blockedText = execution.blockedReason() != null && !execution.blockedReason().isBlank()
                ? execution.blockedReason()
                : "None (Active)";
        facts.child(HTML.div()
                .child(HTML.dt().text("Blocked Reason"))
                .child(HTML.dd().text(blockedText)));

        section.child(facts);

        // 5. Ordered Checkpoints
        HTMLElement checkpointsCard = HTML.div();
        checkpointsCard.child(HTML.h4().text("Ordered Checkpoints"));
        List<QaTaskExecutionCheckpointView> sortedCheckpoints = execution.checkpoints().stream()
                .sorted(Comparator.comparingLong(QaTaskExecutionCheckpointView::sequence))
                .toList();

        if (sortedCheckpoints.isEmpty()) {
            checkpointsCard.child(HTML.p().classes(QaCSS.PUBLIC_BOARD_EMPTY)
                    .text("No execution checkpoints recorded."));
        } else {
            for (QaTaskExecutionCheckpointView checkpoint : sortedCheckpoints) {
                HTMLElement entry = HTML.div().classes(QaCSS.PUBLIC_STAFF_VERIFICATION_FACTS);
                entry.child(HTML.div()
                        .child(HTML.strong().text("Checkpoint #" + checkpoint.sequence() + " · " + checkpoint.status()))
                        .child(HTML.span().text(checkpoint.providerId() + " · " + DATE_FORMATTER.format(checkpoint.recordedAt()))));
                if (checkpoint.nextAction() != null && !checkpoint.nextAction().isBlank()) {
                    entry.child(HTML.p().text("Next action: " + checkpoint.nextAction()));
                }
                entry.child(HTML.p().text("Checkpoint ID: " + checkpoint.checkpointId()));
                checkpointsCard.child(entry);
            }
        }
        section.child(checkpointsCard);

        // 6. Compile / Test / CI Evidence Links
        HTMLElement evidenceLinksSection = HTML.div();
        evidenceLinksSection.child(HTML.h4().text("Compile, Test & CI Evidence"));
        if (execution.evidenceLinks().isEmpty()) {
            evidenceLinksSection.child(HTML.p().classes(QaCSS.PUBLIC_BOARD_EMPTY)
                    .text("No compile, test, or CI evidence links available."));
        } else {
            for (QaTaskExecutionEvidenceLink link : execution.evidenceLinks()) {
                HTMLElement linkItem = HTML.div().classes(QaCSS.PUBLIC_STAFF_VERIFICATION_FACTS);
                linkItem.child(HTML.div()
                        .child(HTML.strong().text(link.label()))
                        .child(HTML.span().text(link.type().name())));
                linkItem.child(HTML.a()
                        .attribute(HTMLAttribute.HREF, link.url())
                        .text("Open " + link.type().name().toLowerCase() + " evidence"));
                evidenceLinksSection.child(linkItem);
            }
        }
        section.child(evidenceLinksSection);

        // 7. Actions: Resume / Retry / Cancel
        // Actions must respect the existing role checks (contributor/QA reviewer/admin)
        boolean allowResume = (isAdmin || isQaReviewer || isContributor) && execution.canResume();
        boolean allowRetry = (isAdmin || isQaReviewer || isContributor) && execution.canRetry();
        boolean allowCancel = (isAdmin || isQaReviewer) && execution.canCancel();

        if (allowResume || allowRetry || allowCancel) {
            HTMLElement actionsDiv = HTML.div().classes(QaCSS.PUBLIC_STAFF_FORM_ACTIONS);

            UUID parsedTaskId;
            try {
                parsedTaskId = UUID.fromString(taskId);
            } catch (IllegalArgumentException ignored) {
                parsedTaskId = null;
            }

            String resumeUrl = parsedTaskId != null ? QaRoutes.executionResume(parsedTaskId) : QaRoutes.EXECUTION_ROOT + "/resume";
            String retryUrl = parsedTaskId != null ? QaRoutes.executionRetry(parsedTaskId) : QaRoutes.EXECUTION_ROOT + "/retry";
            String cancelUrl = parsedTaskId != null ? QaRoutes.executionCancel(parsedTaskId) : QaRoutes.EXECUTION_ROOT + "/cancel";

            if (allowResume) {
                actionsDiv.child(executionActionForm(taskId, resumeUrl, "Resume execution", QaCSS.GOLD_BUTTON, execution.runId(), context));
            }
            if (allowRetry) {
                actionsDiv.child(executionActionForm(taskId, retryUrl, "Retry execution", QaCSS.GOLD_BUTTON, execution.runId(), context));
            }
            if (allowCancel) {
                actionsDiv.child(executionActionForm(taskId, cancelUrl, "Cancel execution", QaCSS.GHOST_BUTTON, execution.runId(), context));
            }

            section.child(actionsDiv);
        }

        return section;
    }

    private static HTMLElement executionActionForm(
            String taskId,
            String actionUrl,
            String label,
            QaCSS buttonClass,
            UUID runId,
            QaTaskPageContext context
    ) {
        HTMLElement form = HTML.form().classes(QaCSS.PUBLIC_STAFF_FORM)
                .attribute(HTMLAttribute.METHOD, "post")
                .attribute(HTMLAttribute.ACTION, actionUrl);
        addRequestContext(form, context);
        form.child(hidden("taskId", taskId));
        if (runId != null) {
            form.child(hidden("runId", runId.toString()));
        }
        form.child(HTML.button().classes(buttonClass)
                .attribute(HTMLAttribute.TYPE, "submit")
                .text(label));
        return form;
    }

    private static void addRequestContext(HTMLElement form, QaTaskPageContext context) {
        if (context == null) {
            return;
        }
        if (!context.csrfToken().isBlank()) {
            form.child(hidden(context.csrfParameterName(), context.csrfToken()));
        }
        context.organizationId().ifPresent(id -> form.child(hidden("organizationId", id.toString())));
    }

    private static HTMLElement submitButton(String label) {
        return HTML.div().classes(QaCSS.PUBLIC_STAFF_FORM_ACTIONS)
                .child(HTML.button().classes(QaCSS.GOLD_BUTTON)
                        .attribute(HTMLAttribute.TYPE, "submit")
                        .text(label));
    }

    private static HTMLElement hidden(String name, String value) {
        return HTML.input().attribute(HTMLAttribute.TYPE, "hidden")
                .attribute(HTMLAttribute.NAME, name)
                .attribute(HTMLAttribute.VALUE, value);
    }

    private static String workspaceHref(String projectKey, String taskId, QaTaskPageContext context) {
        String href = "/workspace/" + projectKey + "/tasks/" + taskId;
        return context.organizationId().map(id -> href + "?organizationId=" + id).orElse(href);
    }

    private static String organizationHref(String path, QaTaskPageContext context) {
        if (context == null) {
            return path;
        }
        return context.organizationId().map(id -> path + "?organizationId=" + id).orElse(path);
    }
}
