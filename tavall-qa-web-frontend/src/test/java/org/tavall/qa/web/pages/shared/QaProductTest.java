package org.tavall.qa.web.pages.shared;

import org.junit.jupiter.api.Test;
import org.tavall.qa.web.routes.QaRoutes;
import org.tavall.web.frontend.design.ForgeLink;
import org.tavall.web.frontend.design.ForgeProduct;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct house-style test for {@link QaProduct}: the QA surface identity and its navigation
 * within the shared Forge chrome. No collaborators are faked.
 */
final class QaProductTest {

    @Test
    void productCarriesQaIdentityAndNavigation() {
        ForgeProduct product = QaProduct.product();
        assertEquals("Tavall QA", product.name());

        List<ForgeLink> nav = product.navigation();
        assertTrue(nav.stream().anyMatch(link -> QaRoutes.ROOT.equals(link.href())));
        assertTrue(nav.stream().anyMatch(link -> QaRoutes.MC.equals(link.href())));
        assertTrue(nav.stream().anyMatch(link -> QaRoutes.DOCS.equals(link.href())));
        assertTrue(nav.stream().anyMatch(link -> QaRoutes.DASHBOARD.equals(link.href())));
    }

    @Test
    void productLinksVenturesStraightToTheDedicatedHost() {
        ForgeProduct product = QaProduct.product();

        List<ForgeLink> nav = product.navigation();
        assertEquals("https://ventures.tavall.org/",
                nav.stream().filter(link -> "Ventures".equals(link.label())).findFirst().orElseThrow().href());
        assertEquals("https://ventures.tavall.org/",
                product.footerLinks().stream().filter(link -> "Ventures".equals(link.label()))
                        .findFirst().orElseThrow().href());
    }

    @Test
    void productMarksCurrentRoute() {
        ForgeProduct product = QaProduct.product(QaRoutes.DOCS);
        assertTrue(product.navigation().stream()
                .filter(link -> QaRoutes.DOCS.equals(link.href()))
                .findFirst()
                .orElseThrow()
                .current());
    }

    @Test
    void venturesProductCarriesTheVenturesIdentityAndCrossHostNavigation() {
        ForgeProduct product = QaProduct.venturesProduct();
        assertEquals("Tavall Ventures", product.name());
        assertEquals("Ventures", product.shortName());
        assertEquals("Public portfolio tracker for Tavall ventures and live systems.", product.tagline());

        List<ForgeLink> nav = product.navigation();
        assertEquals("https://qa.tavall.org/",
                nav.stream().filter(link -> "Projects".equals(link.label())).findFirst().orElseThrow().href());
        assertEquals("/ventures",
                nav.stream().filter(link -> "Ventures".equals(link.label())).findFirst().orElseThrow().href());
        assertTrue(nav.stream().anyMatch(link -> QaRoutes.MC.equals(link.href())));
        assertTrue(nav.stream().anyMatch(link -> QaRoutes.DOCS.equals(link.href())));
        assertTrue(nav.stream().anyMatch(link -> QaRoutes.DASHBOARD.equals(link.href())));
    }

    @Test
    void venturesProductMarksCurrentRoute() {
        ForgeProduct product = QaProduct.venturesProduct(QaRoutes.VENTURES);
        assertTrue(product.navigation().stream()
                .filter(link -> QaRoutes.VENTURES.equals(link.href()))
                .findFirst()
                .orElseThrow()
                .current());
    }
}
