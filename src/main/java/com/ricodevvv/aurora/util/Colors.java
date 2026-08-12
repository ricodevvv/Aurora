package com.ricodevvv.aurora.util;

import org.bukkit.Color;

import java.util.Arrays;
import java.util.List;

/** Utilidades de color: hex, interpolacion, degradados y paletas listas. */
public final class Colors {

    public static final List<Color> FIRE = Arrays.asList(
            Color.fromRGB(255, 240, 120), Color.fromRGB(255, 170, 30),
            Color.fromRGB(230, 70, 20), Color.fromRGB(120, 20, 10));

    public static final List<Color> ICE = Arrays.asList(
            Color.fromRGB(230, 250, 255), Color.fromRGB(140, 220, 255),
            Color.fromRGB(60, 150, 230), Color.fromRGB(30, 80, 170));

    public static final List<Color> TOXIC = Arrays.asList(
            Color.fromRGB(220, 255, 120), Color.fromRGB(120, 230, 60),
            Color.fromRGB(40, 150, 40));

    public static final List<Color> VOID = Arrays.asList(
            Color.fromRGB(190, 120, 255), Color.fromRGB(110, 40, 200),
            Color.fromRGB(30, 10, 60));

    private Colors() {
    }

    /** Acepta "#FF8800" o "FF8800". */
    public static Color hex(String hex) {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        int rgb = Integer.parseInt(clean, 16);
        return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    /** Mezcla dos colores. t = 0 devuelve el primero, t = 1 el segundo. */
    public static Color lerp(Color from, Color to, double t) {
        t = clamp01(t);
        return Color.fromRGB(
                (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t));
    }

    /** Degradado sobre una lista de colores. Ideal para trails que cambian de tono. */
    public static Color gradient(List<Color> palette, double t) {
        if (palette.isEmpty()) return Color.fromRGB(255, 255, 255);
        if (palette.size() == 1) return palette.get(0);
        t = clamp01(t);
        double scaled = t * (palette.size() - 1);
        int index = (int) Math.floor(scaled);
        if (index >= palette.size() - 1) return palette.get(palette.size() - 1);
        return lerp(palette.get(index), palette.get(index + 1), scaled - index);
    }

    /** Arcoiris continuo. hue en 0..1. */
    public static Color rainbow(double hue) {
        java.awt.Color awt = java.awt.Color.getHSBColor((float) (hue % 1.0), 1f, 1f);
        return Color.fromRGB(awt.getRed(), awt.getGreen(), awt.getBlue());
    }

    /** Variacion aleatoria alrededor de un color, para que un efecto no se vea plano. */
    public static Color jitter(Color base, int amount) {
        java.util.Random random = java.util.concurrent.ThreadLocalRandom.current();
        return Color.fromRGB(
                clamp(base.getRed() + random.nextInt(amount * 2 + 1) - amount),
                clamp(base.getGreen() + random.nextInt(amount * 2 + 1) - amount),
                clamp(base.getBlue() + random.nextInt(amount * 2 + 1) - amount));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
