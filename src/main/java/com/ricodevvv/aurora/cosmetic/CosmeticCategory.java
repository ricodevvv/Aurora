package com.ricodevvv.aurora.cosmetic;

/**
 * Cosmetic categories. A player may wear at most one cosmetic per category, so
 * equipping a second one in the same category replaces the first.
 */
public enum CosmeticCategory {

    /** Balloons floating on a lead beside the player. */
    BALLOON("Balloons"),
    /** Companions that follow the player around. */
    PET("Pets"),
    /** Ambient particle effects worn by the player. */
    PARTICLE_EFFECT("Effects"),
    /** Particles left behind while walking. */
    TRAIL("Trails"),
    /** One-shot effects played on death. */
    DEATH_EFFECT("Death Effects"),
    /** Wings rendered on the player's back. */
    WINGS("Wings");

    private final String display;

    CosmeticCategory(String display) {
        this.display = display;
    }

    /**
     * @return a human-readable category name, suitable for menu titles
     */
    public String display() {
        return display;
    }
}
