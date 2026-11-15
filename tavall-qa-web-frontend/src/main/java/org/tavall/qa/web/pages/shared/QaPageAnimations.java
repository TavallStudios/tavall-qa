package org.tavall.qa.web.pages.shared;

import org.tavall.qa.web.generated.QaBoardTS;
import org.tavall.web.api.animation.PageAnimationPlan;
import org.tavall.web.frontend.animation.Animations;
import org.tavall.web.frontend.design.ForgeAnimations;

/** Page-load behavior shared by the existing QA public and staff pages. */
public final class QaPageAnimations {
    private QaPageAnimations() {
    }

    public static PageAnimationPlan build() {
        Animations.Builder animations = Animations.builder();
        ForgeAnimations.reveal().animations()
                .forEach(animation -> animations.onPageLoad(animation.invocation()));
        animations.onPageLoad(QaBoardTS.initializeQaBoard());
        return animations.build();
    }
}
