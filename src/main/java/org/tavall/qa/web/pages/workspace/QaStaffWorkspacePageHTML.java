package org.tavall.qa.web.pages.workspace;

import org.tavall.qa.domain.TaskBoardActionType;
import org.tavall.qa.domain.TaskBoardCommentType;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
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

import java.util.List;

/**
 * Direction C Focus Workspace layout:
 * Modes: Overview, QA, Edit, Thread.
 * URL-hash focus restoration, soft controls, state preview selector, and panel isolation.
 */
public final class QaStaffWorkspacePageHTML {

    private QaStaffWorkspacePageHTML() {
    }

    public static HTMLDocument build(
            TaskBoardTaskView taskView,
            String activeFocus,
            String statePreview,
            boolean canHumanVerify,
            String verifierUuid
    ) {
        if (taskView == null || taskView.task() == null) {
            return renderEmpty();
        }
        TaskBoardTask task = taskView.task();
        return renderWorkspace(
                task.projectKey(),
                task.taskId().toString(),
                task.title(),
                task.status().name(),
                task.qaStatus().name(),
                task.audience().name(),
                activeFocus,
                statePreview,
                canHumanVerify,
                verifierUuid
        );
    }

    public static HTMLDocument build(
            TaskBoardTaskPageView taskPage,
            String activeFocus,
            String statePreview,
            boolean canHumanVerify,
            String verifierUuid
    ) {
        if (taskPage == null) {
            return renderEmpty();
        }
        return renderWorkspace(
                taskPage.projectKey(),
                taskPage.taskId().toString(),
                taskPage.title(),
                taskPage.status().name(),
                taskPage.qaStatus().name(),
                taskPage.audience().name(),
                activeFocus,
                statePreview,
                canHumanVerify,
                verifierUuid
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

    private static HTMLDocument renderWorkspace(
            String projectKey,
            String taskIdStr,
            String title,
            String statusName,
            String qaStatusName,
            String audienceName,
            String activeFocus,
            String statePreview,
            boolean canHumanVerify,
            String verifierUuid
    ) {
        String focus = activeFocus != null ? activeFocus.toUpperCase() : "OVERVIEW";
        String state = statePreview != null ? statePreview.toUpperCase() : "CURRENT";

        HTMLElement shell = HTML.div().classes(
                QaCSS.QA_APP,
                QaCSS.PUBLIC_TASK_BOARD_PAGE,
                QaCSS.PUBLIC_BOARD_MAIN,
                TavallForgeCSS.FORGE_REVEAL
        );

        // 1. Direction C Focus Workspace Toolbar
        HTMLElement toolbar = HTML.section().classes(QaCSS.PUBLIC_BOARD_STAFF_STRIP, QaCSS.PUBLIC_STAFF_WORKSPACE_TOOLBAR);
        HTMLElement toolShell = HTML.div().classes(QaCSS.PUBLIC_BOARD_SHELL);

        HTMLElement toolTop = HTML.div().classes(QaCSS.PUBLIC_STAFF_WORKSPACE_TOOLBAR_TOP);
        HTMLElement titleBox = HTML.div().classes(QaCSS.PUBLIC_STAFF_WORKSPACE_TITLE);
        titleBox.child(HTML.span().text("Staff task workspace"));
        titleBox.child(HTML.strong().text("Direction C · Focus Workspace"));
        toolTop.child(titleBox);

        // Focus Mode Tabs: Overview, QA, Edit, Thread
        HTMLElement navTabs = HTML.nav().classes(QaCSS.PUBLIC_STAFF_VARIANT_TABS)
                .attribute(HTMLAttribute.ARIA_LABEL, "Focus modes");

        List<String> focusModes = List.of("OVERVIEW", "QA", "EDIT", "THREAD");
        for (String mode : focusModes) {
            HTMLElement tab = HTML.button().classes(QaCSS.GHOST_BUTTON)
                    .attribute(HTMLAttribute.TYPE, "button")
                    .text(mode.charAt(0) + mode.substring(1).toLowerCase());
            if (mode.equals(focus)) {
                tab.classes(QaCSS.ACTIVE);
            }
            navTabs.child(tab);
        }
        toolTop.child(navTabs);

        // State preview selector
        HTMLElement statePicker = HTML.form().classes(QaCSS.PUBLIC_STAFF_STATE_PICKER);
        HTMLElement stateLabel = HTML.label();
        stateLabel.child(HTML.span().text("Visual state preview"));
        HTMLElement stateSelect = HTML.select();
        List<String> states = List.of(
                "CURRENT", "ACTIVE_NHV_LINKED", "ACTIVE_NHV_UNLINKED", "DONE_NHV",
                "HUMAN_VERIFIED", "PRIVATE", "BLOCKED", "NO_ACTIONS",
                "MULTI_ACTIONS", "EMPTY_THREAD", "POPULATED_THREAD"
        );
        for (String s : states) {
            HTMLElement opt = HTML.option().text(s.replace('_', ' '));
            stateSelect.child(opt);
        }
        stateLabel.child(stateSelect);
        statePicker.child(stateLabel);
        toolTop.child(statePicker);
        toolShell.child(toolTop);

        // State matrix strip
        HTMLElement matrix = HTML.div().classes(QaCSS.PUBLIC_STAFF_STATE_MATRIX);
        matrix.child(HTML.div()
                .child(HTML.span().text("Work"))
                .child(HTML.strong().text(statusName)));
        matrix.child(HTML.div()
                .child(HTML.span().text("QA"))
                .child(HTML.strong().text(qaStatusName)));
        matrix.child(HTML.div()
                .child(HTML.span().text("Visibility"))
                .child(HTML.strong().text(audienceName)));
        matrix.child(HTML.div()
                .child(HTML.span().text("Identity"))
                .child(HTML.strong().text(canHumanVerify ? "LINKED" : "UNLINKED")));
        matrix.child(HTML.div()
                .child(HTML.span().text("Mode"))
                .child(HTML.strong().text(focus)));
        toolShell.child(matrix);
        toolbar.child(toolShell);
        shell.child(toolbar);

        // 2. Task Hero & Summary
        HTMLElement hero = HTML.section().classes(QaCSS.PUBLIC_BOARD_TASK_HERO);
        HTMLElement heroShell = HTML.div().classes(QaCSS.PUBLIC_BOARD_SHELL);
        heroShell.child(HTML.a().classes(QaCSS.PUBLIC_BOARD_BACK)
                .attribute(HTMLAttribute.HREF, "/projects/" + projectKey + "/tasks/" + taskIdStr)
                .text("← Public task page"));
        heroShell.child(HTML.h1().text(title));
        hero.child(heroShell);
        shell.child(hero);

        // 3. Focus Panels
        HTMLElement mainContainer = HTML.main().classes(QaCSS.PUBLIC_BOARD_SHELL);

        // PANEL 1: QA Panel (PASS, FAIL, BLOCK, REOPEN, NOTE, Human Verification)
        HTMLElement qaPanel = HTML.section().classes(QaCSS.PUBLIC_STAFF_WORKSPACE_PANEL);
        qaPanel.child(HTML.h2().text("QA Verification & Actions"));

        HTMLElement qaForm = HTML.form().classes(QaCSS.PUBLIC_STAFF_FORM)
                .attribute(HTMLAttribute.ACTION, "/projects/" + projectKey + "/tasks/" + taskIdStr);

        HTMLElement actionTypeSelect = HTML.select();
        for (String actionName : List.of("PASS", "FAIL", "BLOCK", "REOPEN", "NOTE")) {
            actionTypeSelect.child(HTML.option().text(actionName));
        }
        qaForm.child(HTML.label()
                .child(HTML.span().text("QA Action"))
                .child(actionTypeSelect));

        qaForm.child(HTML.label()
                .child(HTML.span().text("Verifier Minecraft UUID"))
                .child(HTML.input()));

        qaForm.child(HTML.label()
                .child(HTML.span().text("Environment Grant"))
                .child(HTML.input()));

        qaForm.child(HTML.label().classes(QaCSS.WIDE)
                .child(HTML.span().text("QA Verification Notes / Evidence"))
                .child(HTML.textarea()));

        HTMLElement qaActionsRow = HTML.div().classes(QaCSS.PUBLIC_STAFF_FORM_ACTIONS);
        HTMLElement verifyBtn = HTML.button().classes(QaCSS.GOLD_BUTTON)
                .attribute(HTMLAttribute.TYPE, "submit")
                .text("Record QA Decision");
        if (!canHumanVerify) {
            verifyBtn.attribute(HTMLAttribute.DISABLED, "disabled");
        }
        qaActionsRow.child(verifyBtn);
        qaForm.child(qaActionsRow);
        qaPanel.child(qaForm);
        mainContainer.child(qaPanel);

        // PANEL 2: Edit Panel (Task Details & Destinations)
        HTMLElement editPanel = HTML.section().classes(QaCSS.PUBLIC_STAFF_WORKSPACE_PANEL);
        editPanel.child(HTML.h2().text("Edit Task Details"));
        editPanel.child(HTML.p().classes(QaCSS.PUBLIC_WEIGHT_NOTE)
                .text("Notice: Material edits (title, description, criteria, priority) clear human verification and reset status to NHV."));

        HTMLElement editForm = HTML.form().classes(QaCSS.PUBLIC_STAFF_FORM)
                .attribute(HTMLAttribute.ACTION, "/projects/" + projectKey + "/tasks/" + taskIdStr);
        editForm.child(HTML.label()
                .child(HTML.span().text("Title"))
                .child(HTML.input()));
        editForm.child(HTML.label()
                .child(HTML.span().text("Public Title"))
                .child(HTML.input()));
        editForm.child(HTML.label().classes(QaCSS.WIDE)
                .child(HTML.span().text("Description"))
                .child(HTML.textarea()));

        HTMLElement prioritySelect = HTML.select();
        for (TaskBoardTaskPriority p : TaskBoardTaskPriority.values()) {
            prioritySelect.child(HTML.option().text(p.name()));
        }
        editForm.child(HTML.label()
                .child(HTML.span().text("Priority"))
                .child(prioritySelect));

        HTMLElement editActionsRow = HTML.div().classes(QaCSS.PUBLIC_STAFF_FORM_ACTIONS);
        editActionsRow.child(HTML.button().classes(QaCSS.GOLD_BUTTON)
                .attribute(HTMLAttribute.TYPE, "submit")
                .text("Save Task"));
        editForm.child(editActionsRow);
        editPanel.child(editForm);

        // Add destination
        HTMLElement destBox = HTML.div().classes(QaCSS.PUBLIC_STAFF_EDITOR);
        destBox.child(HTML.h3().text("Add Destination / Action Rail Link"));
        HTMLElement destForm = HTML.form().classes(QaCSS.PUBLIC_STAFF_FORM)
                .attribute(HTMLAttribute.ACTION, "/projects/" + projectKey + "/tasks/" + taskIdStr + "/actions");

        HTMLElement destTypeSelect = HTML.select();
        for (TaskBoardActionType t : TaskBoardActionType.values()) {
            destTypeSelect.child(HTML.option().text(t.name()));
        }
        destForm.child(HTML.label()
                .child(HTML.span().text("Action Type"))
                .child(destTypeSelect));
        destForm.child(HTML.label()
                .child(HTML.span().text("Label"))
                .child(HTML.input()));
        destForm.child(HTML.label().classes(QaCSS.WIDE)
                .child(HTML.span().text("Target (URL or Path)"))
                .child(HTML.input()));
        destForm.child(HTML.div().classes(QaCSS.PUBLIC_STAFF_FORM_ACTIONS)
                .child(HTML.button().classes(QaCSS.GOLD_BUTTON).text("Add Destination")));
        destBox.child(destForm);
        editPanel.child(destBox);
        mainContainer.child(editPanel);

        // PANEL 3: Thread Panel (Comments, suggestions, QA notes)
        HTMLElement threadPanel = HTML.section().classes(QaCSS.PUBLIC_STAFF_WORKSPACE_PANEL);
        threadPanel.child(HTML.h2().text("Discussion & QA Thread"));

        HTMLElement commentForm = HTML.form().classes(QaCSS.PUBLIC_STAFF_FORM)
                .attribute(HTMLAttribute.ACTION, "/projects/" + projectKey + "/tasks/" + taskIdStr + "/comments");

        HTMLElement commentTypeSelect = HTML.select();
        for (TaskBoardCommentType ct : TaskBoardCommentType.values()) {
            commentTypeSelect.child(HTML.option().text(ct.name()));
        }
        commentForm.child(HTML.label()
                .child(HTML.span().text("Entry Type"))
                .child(commentTypeSelect));
        commentForm.child(HTML.label().classes(QaCSS.WIDE)
                .child(HTML.span().text("Comment / QA Note Body"))
                .child(HTML.textarea()));
        commentForm.child(HTML.div().classes(QaCSS.PUBLIC_STAFF_FORM_ACTIONS)
                .child(HTML.button().classes(QaCSS.GOLD_BUTTON).text("Post Entry")));
        threadPanel.child(commentForm);
        mainContainer.child(threadPanel);

        shell.child(mainContainer);

        return Forge.page(QaProduct.product(QaRoutes.ROOT), shell);
    }
}
