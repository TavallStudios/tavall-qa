package org.tavall.qa.domain.auth;

public enum QaTier {
    TIER_0(0, "Tier 0 - Onboarding & Scoped Low-Risk", 5),
    TIER_I(1, "Tier I - Assigned Gameplay & DEV", 25),
    TIER_II(2, "Tier II - Broader Assigned UX/Testing", 75),
    TIER_III(3, "Tier III - Trusted Player-Facing QA & Coordination", 150),
    TIER_IV(4, "Tier IV - Senior Testing & Staging Validation", 250),
    TIER_V(5, "Tier V - Master QA Lead & Verified Production Testing", -1);

    private final int level;
    private final String description;
    private final int nextTierVqcGuideline;

    QaTier(int level, String description, int nextTierVqcGuideline) {
        this.level = level;
        this.description = description;
        this.nextTierVqcGuideline = nextTierVqcGuideline;
    }

    public int level() {
        return level;
    }

    public String description() {
        return description;
    }

    public boolean isAtLeast(QaTier other) {
        return this.level >= other.level;
    }

    public java.util.OptionalInt nextTierVqcGuideline() {
        return nextTierVqcGuideline < 0
                ? java.util.OptionalInt.empty()
                : java.util.OptionalInt.of(nextTierVqcGuideline);
    }
}
