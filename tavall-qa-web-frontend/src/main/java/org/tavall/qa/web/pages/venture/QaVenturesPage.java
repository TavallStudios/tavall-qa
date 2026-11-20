package org.tavall.qa.web.pages.venture;

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

import java.util.List;

/** Ventures index at {@code qa.tavall.org/ventures}: one card per venture the viewer may see. */
public final class QaVenturesPage extends AbstractPage<QaVenturesPage> {

    public static QaVenturesPage build(List<QaVentureView> ventures, boolean available) {
        return new QaVenturesPage()
                .title("Ventures | Tavall QA")
                .html(document(ventures, available))
                .animations(QaPageAnimations.build());
    }

    static HTMLDocument document(List<QaVentureView> ventures, boolean available) {
        HTMLElement body;
        if (!available) {
            body = Forge.callout("Board unavailable", "Venture data could not be loaded right now. This is not an empty portfolio.");
        } else if (ventures.isEmpty()) {
            body = Forge.callout("No ventures yet", "Ventures appear here once they are recorded on the board.");
        } else {
            body = Forge.cardGrid(3, ventures.stream().map(QaVenturesPage::card).toList());
        }
        return Forge.page(QaProduct.product(QaRoutes.VENTURES),
                Forge.section("ventures", "TAVALL PORTFOLIO", "Ventures",
                        "Every Tavall venture: what it is for, where it stands and the work still open.",
                        new ForgeLink("All projects", QaRoutes.ROOT), body));
    }

    private static HTMLElement card(QaVentureView venture) {
        String copy = venture.purpose().isBlank() ? "Purpose not recorded yet." : venture.purpose();
        List<String> tags = venture.ownerRepos().isEmpty()
                ? (venture.liveLinks().isEmpty() ? List.of("No live link yet") : List.of("Live"))
                : venture.ownerRepos().stream().limit(3).toList();
        return Forge.card(new ForgeCard("◆", venture.name(), stageLabel(venture.stage()), copy,
                venture.progressPercent() < 0 ? "" : venture.progressPercent() + "%", tags,
                venture.openTaskCount() + " open " + (venture.openTaskCount() == 1 ? "task" : "tasks"),
                "View venture", QaRoutes.venture(venture.key())));
    }

    static String stageLabel(String stage) {
        if (stage.isBlank()) return "Stage not recorded";
        String lower = stage.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    static HTMLElement link(ForgeLink link) {
        return HTML.a().classes(TavallForgeCSS.FORGE_TEXT_LINK)
                .attribute(HTMLAttribute.HREF, link.href())
                .text(link.label());
    }
}
