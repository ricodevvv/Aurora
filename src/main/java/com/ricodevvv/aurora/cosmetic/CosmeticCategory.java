package com.ricodevvv.aurora.cosmetic;

/** Categorias de cosmeticos. Un jugador solo puede llevar uno de cada una. */
public enum CosmeticCategory {

    BALLOON("Globos"),
    PET("Mascotas"),
    PARTICLE_EFFECT("Efectos"),
    TRAIL("Rastros"),
    DEATH_EFFECT("Muerte"),
    WINGS("Alas");

    private final String display;

    CosmeticCategory(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
