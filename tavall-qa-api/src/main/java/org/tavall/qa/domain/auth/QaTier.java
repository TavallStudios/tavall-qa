package org.tavall.qa.domain.auth;

public enum QaTier {
    TIER_0(0, "Tier 0 - Onboarding & Scoped Low-Risk"),
    TIER_I(1, "Tier I - Assigned Gameplay & DEV"),
    TIER_II(2, "Tier II - Broader Assigned UX/Testing"),
    TIER_III(3, "Tier III - Trusted Player-Facing QA & Coordination"),
    TIER_IV(4, "Tier IV - Senior Testing & Staging Validation"),
    TIER_V(5, "Tier V - Master QA Lead & Verified Production Testing");

    private final int level;
    private final String description;

    QaTier(int level, String description) {
        this.level = level;
        this.description = description;
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
}
