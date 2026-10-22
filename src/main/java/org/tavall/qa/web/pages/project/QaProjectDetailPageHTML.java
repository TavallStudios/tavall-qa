package org.tavall.qa.web.pages.project;

import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.view.TaskBoardPublicProjectSummaryView;
import org.tavall.qa.view.TaskBoardPublicProjectView;
import org.tavall.qa.view.TaskBoardPublicSectionView;
import org.tavall.qa.view.TaskBoardPublicTaskView;
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
 * Direction A Project Detail layout:
 * System/section hierarchy, compact Visible/Current/Done/NHV statistics,
 * first 3 tasks initially visible, and 4-task progressive expander.
 */
public final class QaProjectDetailPageHTML {

    private QaProjectDetailPageHTML() {
    }

    public static HTMLDocument build(
            TaskBoardPublicProjectView projectView,
            boolean canManage,
            boolean canHumanVerify,
            int nhvDebtCount,
            int blockedCount
    ) {
        HTMLElement shell = HTML.div().classes(
                QaCSS.QA_APP,
                QaCSS.PUBLIC_TASK_BOARD_PAGE,
                QaCSS.PUBLIC_BOARD_MAIN,
                TavallForgeCSS.FORGE_REVEAL
        );

        if (projectView == null || projectView.summary() == null) {
            shell.child(HTML.div().classes(QaCSS.PUBLIC_BOARD_SHELL, QaCSS.PUBLIC_BOARD_NOTICE)
                    .text("Project not found or unavailable."));
            return Forge.page(QaProduct.product(QaRoutes.ROOT), shell);
        }

        TaskBoardPublicProjectSummaryView summary = projectView.summary();

        // 1. Hero Section (Direction A)
        HTMLElement hero = HTML.section().classes(QaCSS.PUBLIC_BOARD_PROJECT_HERO);
        HTMLElement heroShell = HTML.div().classes(QaCSS.PUBLIC_BOARD_SHELL);

        heroShell.child(HTML.a().classes(QaCSS.PUBLIC_BOARD_BACK)
                .attribute(HTMLAttribute.HREF, QaRoutes.ROOT)
                .text("← All projects"));

        HTMLElement heroGrid = HTML.div().classes(QaCSS.PUBLIC_PROJECT_HERO_GRID);
        HTMLElement copy = HTML.div();
        copy.child(HTML.p().classes(QaCSS.SECTION_KICKER)
                .text(summary.status() + " · Public project"));
        copy.child(HTML.h1().text(summary.displayName()));
        if (summary.description() != null && !summary.description().isBlank()) {
            copy.child(HTML.p().classes(QaCSS.PUBLIC_BOARD_LEAD).text(summary.description()));
        }
        heroGrid.child(copy);

        HTMLElement progress = HTML.div().classes(QaCSS.PUBLIC_PROJECT_PROGRESS);
        progress.child(HTML.strong().text(summary.progressPercent() + "%"));
        progress.child(HTML.span().text("weighted visible progress"));
        HTMLElement meter = HTML.div().classes(QaCSS.PUBLIC_PROJECT_METER);
        meter.child(HTML.span().classes(QaCSS.ACTIVE));
        progress.child(meter);
        heroGrid.child(progress);
        heroShell.child(heroGrid);

        // Stats dl
        HTMLElement stats = HTML.dl().classes(QaCSS.PUBLIC_PROJECT_TASK_STATS);
        stats.child(HTML.div()
                .child(HTML.dt().text("Visible tasks"))
                .child(HTML.dd().text(String.valueOf(summary.taskCount()))));
        stats.child(HTML.div()
                .child(HTML.dt().text("Current"))
                .child(HTML.dd().text(String.valueOf(summary.activeCount()))));
        stats.child(HTML.div()
                .child(HTML.dt().text("Done"))
                .child(HTML.dd().text(String.valueOf(summary.completedCount()))));
        stats.child(HTML.div()
                .child(HTML.dt().text("NHV"))
                .child(HTML.dd().text(String.valueOf(summary.nhvCount()))));
        heroShell.child(stats);

        hero.child(heroShell);
        shell.child(hero);

        // 2. Staff Strip (Progressive Enhancement)
        if (canManage) {
            HTMLElement staffStrip = HTML.section().classes(QaCSS.PUBLIC_BOARD_STAFF_STRIP);
            HTMLElement staffInner = HTML.div().classes(QaCSS.PUBLIC_BOARD_SHELL, QaCSS.PUBLIC_BOARD_STAFF_STRIP_INNER);
            staffInner.child(HTML.div()
                    .child(HTML.span().text("Staff mode"))
                    .child(HTML.strong().text("Controls stay attached to this project.")));
            staffInner.child(HTML.div()
                    .child(HTML.span().text("NHV debt"))
                    .child(HTML.strong().text(nhvDebtCount + " tasks")));
            staffInner.child(HTML.div()
                    .child(HTML.span().text("Blocked"))
                    .child(HTML.strong().text(blockedCount + " tasks")));
            staffInner.child(HTML.a()
                    .attribute(HTMLAttribute.HREF, "#staff-add-task")
                    .text("Add task ↓"));
            staffStrip.child(staffInner);
            shell.child(staffStrip);
        }

        // 3. Tracked Work Section (Systems & Tasks - Direction A)
        HTMLElement workSection = HTML.section().classes(QaCSS.PUBLIC_BOARD_SHELL, QaCSS.PUBLIC_PROJECT_WORK);
        HTMLElement heading = HTML.div().classes(QaCSS.PUBLIC_BOARD_SECTION_HEADING, QaCSS.ROW);
        heading.child(HTML.div()
                .child(HTML.p().classes(QaCSS.SECTION_KICKER).text("Systems & tasks"))
                .child(HTML.h2().text("Tracked work.")));
        heading.child(HTML.p().text("Open a task for evidence, discussion, verification, and its owning edit surfaces."));
        workSection.child(heading);

        List<TaskBoardPublicSectionView> sections = projectView.sections();
        if (sections == null || sections.isEmpty()) {
            workSection.child(HTML.div().classes(QaCSS.PUBLIC_BOARD_EMPTY)
                    .child(HTML.strong().text("No tasks are public in this project yet.")));
        } else {
            HTMLElement systemList = HTML.div().classes(QaCSS.PUBLIC_SYSTEM_TASK_LIST);
            for (TaskBoardPublicSectionView section : sections) {
                HTMLElement group = HTML.section().classes(QaCSS.PUBLIC_SYSTEM_TASK_GROUP);
                group.attribute(HTMLAttribute.ARIA_LABEL, section.sectionKey());

                // Header
                HTMLElement sHead = HTML.header().classes(QaCSS.PUBLIC_SYSTEM_TASK_HEADER);
                sHead.child(HTML.h3().text(section.sectionKey().replace('-', ' ').toUpperCase()));

                HTMLElement sProg = HTML.div().classes(QaCSS.PUBLIC_SYSTEM_TASK_PROGRESS);
                HTMLElement sMeter = HTML.div().classes(QaCSS.PUBLIC_PROJECT_METER);
                sMeter.child(HTML.span().classes(QaCSS.ACTIVE));
                sProg.child(sMeter);

                HTMLElement sMeta = HTML.div().classes(QaCSS.PUBLIC_SYSTEM_TASK_META);
                sMeta.child(HTML.span().text(section.taskCount() + " visible"));
                sMeta.child(HTML.span().text(section.activeCount() + " current"));
                sMeta.child(HTML.span().text(section.completedCount() + " done"));
                sMeta.child(HTML.span().text(section.nhvCount() + " NHV"));
                sProg.child(sMeta);
                sHead.child(sProg);

                sHead.child(HTML.strong().text(section.progressPercent() + "%"));
                group.child(sHead);

                // Task rows (Direction A: 3 initial visible, 4 per expand)
                HTMLElement taskList = HTML.div().classes(QaCSS.PUBLIC_TASK_LIST);
                for (TaskBoardPublicTaskView task : section.tasks()) {
                    HTMLElement row = HTML.article().classes(QaCSS.PUBLIC_TASK_ROW);
                    if (task.status() == TaskBoardTaskStatus.DONE) {
                        row.classes(QaCSS.IS_DONE);
                    }

                    HTMLElement state = HTML.div().classes(QaCSS.PUBLIC_TASK_STATE);
                    String stateChar = task.status() == TaskBoardTaskStatus.DONE ? "✓"
                            : (task.status() == TaskBoardTaskStatus.ACTIVE ? "•" : "○");
                    state.child(HTML.span().text(stateChar));
                    row.child(state);

                    HTMLElement tCopy = HTML.div().classes(QaCSS.PUBLIC_TASK_COPY);
                    HTMLElement tTitleRow = HTML.div().classes(QaCSS.PUBLIC_TASK_TITLE_ROW);
                    tTitleRow.child(HTML.h4()
                            .child(HTML.a()
                                    .attribute(HTMLAttribute.HREF, QaRoutes.task(summary.projectKey(), task.taskId()))
                                    .text(task.title())));
                    tTitleRow.child(HTML.span().text(task.status().name()));
                    tCopy.child(tTitleRow);

                    if (task.summary() != null && !task.summary().isBlank()) {
                        tCopy.child(HTML.p().text(task.summary()));
                    }

                    HTMLElement tMeta = HTML.div().classes(QaCSS.PUBLIC_TASK_META);
                    tMeta.child(HTML.span().text(task.qaStatus().name()));
                    tMeta.child(HTML.span().text(task.priority().name()));
                    tMeta.child(HTML.span().text("Weight " + task.weight()));
                    if (task.sourceType() != null && !task.sourceType().isBlank()) {
                        tMeta.child(HTML.span().text(task.sourceType()));
                    }
                    tMeta.child(HTML.a().classes(QaCSS.PUBLIC_TASK_OPEN)
                            .attribute(HTMLAttribute.HREF, QaRoutes.task(summary.projectKey(), task.taskId()))
                            .text("Open task →"));
                    tCopy.child(tMeta);

                    row.child(tCopy);
                    taskList.child(row);
                }
                group.child(taskList);

                // Expander button (Direction A expand behavior: 3 initial -> 4 more per click)
                HTMLElement expander = HTML.button().classes(QaCSS.PUBLIC_TASK_EXPANDER)
                        .attribute(HTMLAttribute.TYPE, "button")
                        .text("Show more tasks");
                group.child(expander);

                systemList.child(group);
            }
            workSection.child(systemList);
        }
        shell.child(workSection);

        // 4. Staff Add Task Panel
        if (canManage) {
            HTMLElement staffSection = HTML.section().classes(QaCSS.PUBLIC_BOARD_SHELL, QaCSS.PUBLIC_STAFF_EDITOR);
            HTMLElement details = HTML.details();
            details.child(HTML.summary()
                    .child(HTML.span().text("Staff control"))
                    .child(HTML.strong().text("Add tracked work"))
                    .child(HTML.span().text("Backlog + NHV + private")));

            HTMLElement form = HTML.form().classes(QaCSS.PUBLIC_STAFF_FORM)
                    .attribute(HTMLAttribute.ACTION, QaRoutes.TASKS_MUTATION.replace("{projectKey}", summary.projectKey()));
            form.child(HTML.label()
                    .child(HTML.span().text("Section key"))
                    .child(HTML.input()));
            form.child(HTML.label()
                    .child(HTML.span().text("Internal title"))
                    .child(HTML.input()));
            form.child(HTML.label().classes(QaCSS.WIDE)
                    .child(HTML.span().text("Description"))
                    .child(HTML.textarea()));
            form.child(HTML.div().classes(QaCSS.PUBLIC_STAFF_FORM_ACTIONS)
                    .child(HTML.button().classes(QaCSS.GOLD_BUTTON).text("Create NHV task")));
            details.child(form);
            staffSection.child(details);
            shell.child(staffSection);
        }

        return Forge.page(QaProduct.product(QaRoutes.ROOT), shell);
    }
}
