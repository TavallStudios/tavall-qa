package org.tavall.qa.web.pages.shared;

import org.tavall.qa.web.routes.QaRoutes;
import org.tavall.web.frontend.design.ForgeLink;
import org.tavall.web.frontend.design.ForgeProduct;

import java.util.List;

/**
 * Tavall QA identity and navigation within the shared Tavall Forge chrome.
 */
public final class QaProduct {

    private QaProduct() {
    }

    public static ForgeProduct product() {
        return new ForgeProduct(
                "Tavall QA",
                "QA",
                "Public development tracker and staff QA workspace.",
                List.of(
                        new ForgeLink("Projects", QaRoutes.ROOT),
                        new ForgeLink("Ventures", QaRoutes.VENTURES),
                        new ForgeLink("Tavall MC", QaRoutes.MC),
                        new ForgeLink("Docs", QaRoutes.DOCS),
                        new ForgeLink("Dashboard", QaRoutes.DASHBOARD)
                ),
                List.of(),
                List.of(
                        new ForgeLink("Projects", QaRoutes.ROOT),
                        new ForgeLink("Ventures", QaRoutes.VENTURES),
                        new ForgeLink("Tavall MC", QaRoutes.MC),
                        new ForgeLink("Docs", QaRoutes.DOCS),
                        new ForgeLink("Dashboard", QaRoutes.DASHBOARD)
                )
        );
    }

    public static ForgeProduct product(String currentRoute) {
        return product().current(currentRoute);
    }
}
