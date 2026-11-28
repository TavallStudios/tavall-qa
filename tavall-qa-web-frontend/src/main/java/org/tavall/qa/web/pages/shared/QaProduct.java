package org.tavall.qa.web.pages.shared;

import org.tavall.qa.web.routes.QaRoutes;
import org.tavall.web.frontend.design.ForgeLink;
import org.tavall.web.frontend.design.ForgeProduct;

import java.util.List;

/**
 * Tavall QA identity and navigation within the shared Tavall Forge chrome.
 *
 * <p>{@link #product()} is the {@code qa.tavall.org} surface identity. {@link #venturesProduct()}
 * is the dedicated {@code ventures.tavall.org} identity, which links back to the QA host for
 * projects and task boards instead of using relative QA paths.</p>
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

    /** Identity served on the dedicated Ventures host {@code ventures.tavall.org}. */
    public static ForgeProduct venturesProduct() {
        return new ForgeProduct(
                "Tavall Ventures",
                "Ventures",
                "Public portfolio tracker for Tavall ventures and live systems.",
                List.of(
                        new ForgeLink("Projects", QaRoutes.QA),
                        new ForgeLink("Ventures", QaRoutes.VENTURES),
                        new ForgeLink("Tavall MC", QaRoutes.MC),
                        new ForgeLink("Docs", QaRoutes.DOCS),
                        new ForgeLink("Dashboard", QaRoutes.DASHBOARD)
                ),
                List.of(),
                List.of(
                        new ForgeLink("Projects", QaRoutes.QA),
                        new ForgeLink("Ventures", QaRoutes.VENTURES),
                        new ForgeLink("Tavall MC", QaRoutes.MC),
                        new ForgeLink("Docs", QaRoutes.DOCS),
                        new ForgeLink("Dashboard", QaRoutes.DASHBOARD)
                )
        );
    }

    public static ForgeProduct venturesProduct(String currentRoute) {
        return venturesProduct().current(currentRoute);
    }
}
