package org.tavall.qa.web.pages.venture;

import org.tavall.qa.domain.TaskBoardProjectKey;
import org.tavall.qa.domain.TaskBoardVenture;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.handler.ITaskBoardVentureQueryHandler;
import org.tavall.qa.web.pages.shared.QaPageAnimations;
import org.tavall.qa.web.pages.shared.QaProduct;
import org.tavall.qa.web.routes.QaRoutes;
import org.tavall.web.api.html.HTMLAttribute;
import org.tavall.web.api.html.HTMLDocument;
import org.tavall.web.api.html.HTMLElement;
import org.tavall.web.frontend.abstracts.page.AbstractPage;
import org.tavall.web.frontend.design.Forge;
import org.tavall.web.frontend.design.ForgeCard;
import org.tavall.web.frontend.design.ForgeLink;
import org.tavall.web.frontend.generated.TavallForgeCSS;
import org.tavall.web.frontend.html.HTML;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** One venture at {@code qa.tavall.org/ventures/{key}}: purpose, stage, live links, metrics and open tasks. */
public final class QaVenturePage extends AbstractPage<QaVenturePage> {

    public static QaVenturePage build(QaVentureView venture) {
        return new QaVenturePage()
                .title(venture.name() + " | Ventures | Tavall QA")
                .html(document(venture))
                .animations(QaPageAnimations.build());
    }

    /**
     * Builds one venture through the QA venture query contract, including the engineering tasks
     * matched to its owner repositories.
     */
    public static QaVenturePage build(
            ITaskBoardVentureQueryHandler ventures,
            String slug,
            QaSubjectContext subject
    ) {
        Objects.requireNonNull(ventures, "ventures");
        TaskBoardVenture venture = ventures.findVenture(slug, subject)
                .orElseThrow(() -> new IllegalArgumentException("Unknown venture: " + slug));
        return build(QaVentureView.fromVenture(
                venture, ventures.findVentureTasks(slug, subject)));
    }

    static HTMLDocument document(QaVentureView venture) {
        HTMLElement facts = HTML.div().classes(TavallForgeCSS.FORGE_TAG_ROW)
                .child(Forge.pill(QaVenturesPage.stageLabel(venture.stage()), TavallForgeCSS.FORGE_PILL_MOD_ACCENT))
                .child(Forge.pill(venture.openTaskCount() + " open of " + venture.taskCount()));
        if (venture.progressPercent() >= 0) {
            facts.child(Forge.pill(venture.progressPercent() + "% complete"));
        }

        HTMLElement repositories = HTML.ul();
        venture.ownerRepos().forEach(repo -> repositories.child(HTML.li().text(repo)));

        HTMLElement links = HTML.div().classes(TavallForgeCSS.FORGE_TAG_ROW);
        if (venture.liveLinks().isEmpty()) {
            links.child(HTML.p().text("No live link recorded yet."));
        } else {
            venture.liveLinks().forEach(link -> links.child(QaVenturesPage.link(link)));
        }

        HTMLElement metrics;
        if (venture.metrics().isEmpty()) {
            metrics = HTML.p().text("No metrics recorded yet. Only measured values appear here.");
        } else {
            List<HTMLElement> cards = new ArrayList<>();
            for (QaVentureView.Metric metric : venture.metrics()) {
                cards.add(Forge.card(new ForgeCard("▲", metric.value(), metric.label(), "",
                        "", List.of(), metric.source(), "", "")));
            }
            metrics = Forge.cardGrid(cards.size() >= 4 ? 4 : 3, cards);
        }

        HTMLElement tasks;
        if (venture.openTasks().isEmpty()) {
            tasks = HTML.p().text("No open tasks.");
        } else {
            tasks = HTML.ul();
            for (QaVentureView.OpenTask task : venture.openTasks()) {
                tasks.child(HTML.li()
                        .child(HTML.a().classes(TavallForgeCSS.FORGE_TEXT_LINK)
                                .attribute(HTMLAttribute.HREF, task.href())
                                .text(task.title()))
                        .child(Forge.pill(task.status().name())));
            }
        }

        String purpose = venture.purpose().isBlank() ? "Purpose not recorded yet." : venture.purpose();
        String taskBoardKey = venture.ownerRepos().isEmpty()
                ? venture.key()
                : TaskBoardProjectKey.ENGINEERING.value();
        String taskBoardLabel = venture.ownerRepos().isEmpty()
                ? "Full project board"
                : "Engineering task board";
        return Forge.page(QaProduct.product(QaRoutes.VENTURES),
                Forge.section("venture", "VENTURE", venture.name(), purpose,
                        new ForgeLink("All ventures", QaRoutes.VENTURES), facts),
                Forge.section("venture-repos", "SYSTEMS", "Owner repositories", "", null, repositories),
                Forge.section("venture-links", "LIVE", "Where it runs", "", null, links),
                Forge.section("venture-blocker", "NEXT", "Top blocker", "", null,
                        HTML.p().text(venture.topBlocker().isBlank() ? "No blocker recorded." : venture.topBlocker())),
                Forge.section("venture-metrics", "METRICS", "Measured so far", "", null, metrics),
                Forge.section("venture-tasks", "OPEN WORK", "Open tasks", "",
                        new ForgeLink(taskBoardLabel, QaRoutes.project(taskBoardKey)), tasks));
    }
}
