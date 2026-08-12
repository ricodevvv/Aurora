package com.ricodevvv.aurora.hologram;

import org.bukkit.ChatColor;
import org.bukkit.entity.ArmorStand;

/** Linea de texto plano. */
public class TextLine extends HologramLine {

    private String text;

    public TextLine(String text) {
        this.text = color(text);
    }

    @Override
    public double height() {
        return 0.25;
    }

    @Override
    protected void apply(ArmorStand stand) {
        stand.setCustomName(text);
        stand.setCustomNameVisible(true);
    }

    public void setText(String text) {
        this.text = color(text);
        if (stand != null && stand.isValid()) {
            stand.setCustomName(this.text);
            stand.setCustomNameVisible(!this.text.isEmpty());
        }
    }

    public String getText() {
        return text;
    }

    private static String color(String input) {
        return input == null ? "" : ChatColor.translateAlternateColorCodes('&', input);
    }
}
