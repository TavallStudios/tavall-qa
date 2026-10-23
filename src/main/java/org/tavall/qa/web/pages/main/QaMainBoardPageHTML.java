package org.tavall.qa.web.pages.main;

import org.tavall.qa.view.TaskBoardPublicDashboardView;
import org.tavall.qa.view.TaskBoardPublicProjectSummaryView;
import org.tavall.qa.view.TaskBoardQaProgressionView;
import org.tavall.qa.view.TaskBoardTaskPageView;
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
 * Direction C synthesis: Project-only index with concise "Follow what changed" language,
 * weighted overall progress, and compact status metrics.
 */
public final class QaMainBoardPageHTML {

    private QaMainBoardPageHTML() {
    }

    public static HTMLDocument build(
            TaskBoardPublicDashboardView dashboard,
            boolean canManage,
            QaMainBoardPageContext context
    ) {
        if (context == null) {
            context = QaMainBoardPageContext.anonymous();
        }
        HTMLElement shell = HTML.div().classes(
                QaCSS.QA_APP,
                QaCSS.PUBLIC_TASK_BOARD_PAGE,
                QaCSS.PUBLIC_BOARD_MAIN,
                TavallForgeCSS.FORGE_REVEAL
        );

        // 1. Hero Section (Direction C Synthesis)
        HTMLElement hero = HTML.section().classes(QaCSS.PUBLIC_BOARD_HERO);
        HTMLElement heroShell = HTML.div().classes(QaCSS.PUBLIC_BOARD_SHELL);
        heroShell.child(HTML.p().classes(QaCSS.SECTION_KICKER).text("Public development tracker"));
        heroShell.child(HTML.h1().text("Follow what changed."));
        heroShell.child(HTML.p().classes(QaCSS.PUBLIC_BOARD_LEAD)
                .text("Project-level progress across Tavall. Open a project to see its systems and tasks."));

        if (dashboard != null && dashboard.storageAvailable()) {
            HTMLElement stats = HTML.dl().classes(QaCSS.PUBLIC_INDEX_STATS);
            stats.child(HTML.div()
                    .child(HTML.dt().text("Projects"))
                    .child(HTML.dd().text(String.valueOf(dashboard.projects().size()))));
            stats.child(HTML.div()
                    .child(HTML.dt().text("Overall"))
                    .child(HTML.dd().text(dashboard.progressPercent() + "%")));
            stats.child(HTML.div()
                    .child(HTML.dt().text("Current"))
                    .child(HTML.dd().text(String.valueOf(dashboard.activeCount()))));
            stats.child(HTML.div()
                    .child(HTML.dt().text("NHV"))
                    .child(HTML.dd().text(String.valueOf(dashboard.nhvCount()))));
            heroShell.child(stats);
        }
        hero.child(heroShell);
        shell.child(hero);

        // 2. Staff Strip (Progressive enhancement)
        if (canManage || context.canManageProjects()) {
            HTMLElement staffStrip = HTML.section().classes(QaCSS.PUBLIC_BOARD_STAFF_STRIP);
            HTMLElement staffInner = HTML.div().classes(QaCSS.PUBLIC_BOARD_SHELL, QaCSS.PUBLIC_BOARD_STAFF_STRIP_INNER);
            staffInner.child(HTML.div()
                    .child(HTML.span().text("Staff mode"))
                    .child(HTML.strong().text("Same public board, additional controls.")));
            staffInner.child(HTML.div()
                    .child(HTML.span().text("Identity"))
                    .child(HTML.strong().text(context.identityStatus())));
            if (context.canManageProjects()) {
                staffInner.child(HTML.a()
                        .attribute(HTMLAttribute.HREF, "#staff-create-project")
                        .text("Create project ↓"));
            }
            staffStrip.child(staffInner);
            shell.child(staffStrip);
        }

        context.qaProgression().ifPresent(progression -> shell.child(qaProgression(progression)));

        // Storage notice if unavailable
        if (dashboard == null || !dashboard.storageAvailable()) {
            shell.child(HTML.div().classes(QaCSS.PUBLIC_BOARD_SHELL, QaCSS.PUBLIC_BOARD_NOTICE)
                    .text("Task Board storage is temporarily unavailable."));
            return Forge.page(QaProduct.product(QaRoutes.ROOT), shell);
        }

        // 3. Project Index Grid
        HTMLElement indexSection = HTML.section().classes(QaCSS.PUBLIC_PROJECT_INDEX);
        HTMLElement indexShell = HTML.div().classes(QaCSS.PUBLIC_BOARD_SHELL);

        HTMLElement heading = HTML.div().classes(QaCSS.PUBLIC_BOARD_SECTION_HEADING, QaCSS.ROW);
        heading.child(HTML.div()
                .child(HTML.p().classes(QaCSS.SECTION_KICKER).text("Projects"))
                .child(HTML.h2().text("Public work.")));
        heading.child(HTML.p().text("Open a project for its systems, tasks, discussion, and verification state."));
        indexShell.child(heading);

        List<TaskBoardPublicProjectSummaryView> projects = dashboard.projects();
        if (projects == null || projects.isEmpty()) {
            indexShell.child(HTML.div().classes(QaCSS.PUBLIC_BOARD_EMPTY)
                    .child(HTML.strong().text("No public projects are published yet.")));
        } else {
            HTMLElement grid = HTML.div().classes(QaCSS.PUBLIC_PROJECT_INDEX_GRID);
            for (TaskBoardPublicProjectSummaryView project : projects) {
                HTMLElement card = HTML.a().classes(QaCSS.PUBLIC_PROJECT_INDEX_CARD)
                        .attribute(HTMLAttribute.HREF, QaRoutes.project(project.projectKey()));

                HTMLElement copy = HTML.div().classes(QaCSS.PUBLIC_PROJECT_INDEX_COPY);
                copy.child(HTML.span().text(project.status().name()));
                copy.child(HTML.h3().text(project.displayName()));
                if (project.description() != null && !project.description().isBlank()) {
                    copy.child(HTML.p().text(project.description()));
                }
                card.child(copy);

                HTMLElement progress = HTML.div().classes(QaCSS.PUBLIC_PROJECT_INDEX_PROGRESS);
                HTMLElement meterWrap = HTML.div();
                HTMLElement meter = HTML.div().classes(QaCSS.PUBLIC_PROJECT_METER);
                meter.child(HTML.span().text("").classes(QaCSS.ACTIVE));
                meterWrap.child(meter);

                HTMLElement meta = HTML.div().classes(QaCSS.PUBLIC_PROJECT_INDEX_META);
                meta.child(HTML.span().text(project.taskCount() + " visible"));
                meta.child(HTML.span().text(project.activeCount() + " current"));
                meta.child(HTML.span().text(project.completedCount() + " done"));
                meterWrap.child(meta);

                progress.child(meterWrap);
                progress.child(HTML.strong().text(project.progressPercent() + "%"));
                card.child(progress);

                grid.child(card);
            }
            indexShell.child(grid);
        }
        indexSection.child(indexShell);
        shell.child(indexSection);

        if (!context.assignedTasks().isEmpty()) {
            HTMLElement assignedSection = HTML.section().classes(QaCSS.PUBLIC_PROJECT_INDEX);
            HTMLElement assignedShell = HTML.div().classes(QaCSS.PUBLIC_BOARD_SHELL);
            assignedShell.child(HTML.div().classes(QaCSS.PUBLIC_BOARD_SECTION_HEADING, QaCSS.ROW)
                    .child(HTML.div()
                            .child(HTML.p().classes(QaCSS.SECTION_KICKER).text("QA assignments"))
                            .child(HTML.h2().text("Work assigned to you.")))
                    .child(HTML.p().text("Open an assigned task to submit evidence or continue its review.")));
            HTMLElement assignedGrid = HTML.div().classes(QaCSS.PUBLIC_PROJECT_INDEX_GRID);
            for (TaskBoardTaskPageView task : context.assignedTasks()) {
                String taskUrl = QaRoutes.task(task.projectKey(), task.taskId());
                if (context.organizationId().isPresent()) {
                    taskUrl += "?organizationId=" + context.organizationId().get();
                }
                HTMLElement card = HTML.a().classes(QaCSS.PUBLIC_PROJECT_INDEX_CARD)
                        .attribute(HTMLAttribute.HREF, taskUrl);
                card.child(HTML.div().classes(QaCSS.PUBLIC_PROJECT_INDEX_COPY)
                        .child(HTML.span().text(task.status().name() + " · " + task.audience().name()))
                        .child(HTML.h3().text(task.title()))
                        .child(HTML.p().text(task.summary())));
                card.child(HTML.div().classes(QaCSS.PUBLIC_PROJECT_INDEX_META)
                        .child(HTML.span().text(task.sectionKey().replace('-', ' ')))
                        .child(HTML.span().text("QA " + task.qaStatus().name())));
                assignedGrid.child(card);
            }
            assignedShell.child(assignedGrid);
            assignedSection.child(assignedShell);
            shell.child(assignedSection);
        }

        // 4. Staff Create Project Panel (Direction C Progressive Enhancement)
        if (context.canManageProjects()) {
            HTMLElement staffSection = HTML.section().classes(QaCSS.PUBLIC_BOARD_SHELL, QaCSS.PUBLIC_STAFF_EDITOR);
            HTMLElement details = HTML.details();
            details.child(HTML.summary()
                    .child(HTML.span().text("Staff control"))
                    .child(HTML.strong().text("Create Task Board project"))
                    .child(HTML.span().text("Open editor")));

            HTMLElement form = HTML.form().classes(QaCSS.PUBLIC_STAFF_FORM)
                    .attribute(HTMLAttribute.METHOD, "post")
                    .attribute(HTMLAttribute.ACTION, QaRoutes.PROJECTS_MUTATION);
            if (!context.csrfToken().isBlank()) {
                form.child(hidden(context.csrfParameterName(), context.csrfToken()));
            }
            context.organizationId().ifPresent(id -> form.child(hidden("organizationId", id.toString())));
            form.child(HTML.label()
                    .child(HTML.span().text("Project key"))
                    .child(HTML.input().attribute(HTMLAttribute.NAME, "projectKey").attribute(HTMLAttribute.REQUIRED, "required")));
            form.child(HTML.label()
                    .child(HTML.span().text("Display name"))
                    .child(HTML.input().attribute(HTMLAttribute.NAME, "displayName").attribute(HTMLAttribute.REQUIRED, "required")));
            form.child(HTML.label().classes(QaCSS.WIDE)
                    .child(HTML.span().text("Description"))
                    .child(HTML.textarea().attribute(HTMLAttribute.NAME, "description")));
            form.child(HTML.div().classes(QaCSS.PUBLIC_STAFF_FORM_ACTIONS)
                    .child(HTML.button().classes(QaCSS.GOLD_BUTTON)
                            .attribute(HTMLAttribute.TYPE, "submit")
                            .text("Create project")));
            details.child(form);
            staffSection.child(details);
            shell.child(staffSection);
        }

        return Forge.page(QaProduct.product(QaRoutes.ROOT), shell);
    }

    private static HTMLElement hidden(String name, String value) {
        return HTML.input().attribute(HTMLAttribute.TYPE, "hidden")
                .attribute(HTMLAttribute.NAME, name)
                .attribute(HTMLAttribute.VALUE, value);
    }

    private static HTMLElement qaProgression(TaskBoardQaProgressionView progression) {
        HTMLElement section = HTML.section().classes(QaCSS.PUBLIC_BOARD_SHELL, QaCSS.PUBLIC_SECTION_CARD);
        section.child(HTML.p().classes(QaCSS.SECTION_KICKER).text("Your QA progression"));
        HTMLElement stats = HTML.dl().classes(QaCSS.PUBLIC_INDEX_STATS);
        stats.child(HTML.div()
                .child(HTML.dt().text("Accepted QA/QC tasks"))
                .child(HTML.dd().text(Integer.toString(progression.acceptedQaQcTaskCount()))));
        stats.child(HTML.div()
                .child(HTML.dt().text("VQC"))
                .child(HTML.dd().text(Integer.toString(progression.vqcEarned()))));
        stats.child(HTML.div()
                .child(HTML.dt().text("QA tier"))
                .child(HTML.dd().text(progression.currentTier().map(Enum::name)
                        .orElse(progression.qaClasses().contains(org.tavall.qa.domain.auth.QaClass.FOUNDING_PUBLIC)
                                ? "Founding Public Tester"
                                : "Not assigned"))));
        progression.nextTierVqcGuideline().ifPresent(guideline -> stats.child(HTML.div()
                .child(HTML.dt().text("Next-tier VQC guideline"))
                .child(HTML.dd().text(Integer.toString(guideline)))));
        section.child(stats);
        section.child(HTML.p().classes(QaCSS.PUBLIC_WEIGHT_NOTE)
                .text("VQC records accepted contributions. Promotions and capability grants remain separately reviewed."));
        return section;
    }
}
