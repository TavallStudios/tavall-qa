package org.tavall.qa.web.pages.task;

import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
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
                task.weight(),
                task.humanVerified(),
                task.verifiedByMinecraftUUID() != null ? task.verifiedByMinecraftUUID().toString() : null,
                task.humanVerifiedAt(),
                task.hostValidationRequired(),
                taskView.actions(),
                canManage,
                canHumanVerify
        );
    }

    public static HTMLDocument build(
            TaskBoardTaskPageView taskPage,
            boolean canManage,
            boolean canHumanVerify
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
                taskPage.weight(),
                taskPage.qaStatus() == TaskBoardQaStatus.HUMAN_VERIFIED,
                null,
                null,
                false,
                List.of(),
                canManage,
                canHumanVerify
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
            int weight,
            boolean isHumanVerified,
            String verifierUuid,
            java.time.Instant humanVerifiedAt,
            boolean hostValidationRequired,
            List<TaskBoardResolvedActionView> actions,
            boolean canManage,
            boolean canHumanVerify
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

        heroShell.child(HTML.a().classes(QaCSS.PUBLIC_BOARD_BACK)
                .attribute(HTMLAttribute.HREF, QaRoutes.project(projectKey))
                .text("← " + projectDisplayName + " board"));

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
                    .attribute(HTMLAttribute.HREF, "/workspace/" + projectKey + "/tasks/" + taskIdStr)
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
}
