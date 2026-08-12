package com.ricodevvv.aurora.hologram;

import com.ricodevvv.aurora.animation.Animation;
import com.ricodevvv.aurora.util.Easing;
import org.bukkit.Location;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Texto flotante de un solo uso: aparece, sube y se borra solo.
 * Es el clasico indicador de dano de los practice.
 *
 * Cada llamada crea y destruye un ArmorStand. En un combate 1v1 no pasa nada,
 * pero si vas a spamear esto en un evento de 100 jugadores considera pool o
 * limitar por jugador.
 */
public final class FloatingText {

    private FloatingText() {
    }

    /** Texto que sube y desaparece. */
    public static Animation spawn(Location at, String text, int ticks, double riseHeight) {
        TextLine line = new TextLine(text);
        Location start = at.clone().add(
                rand(0.4), rand(0.2), rand(0.4)); // dispersion para que no se encimen
        line.spawn(start);

        return new Animation() {
            @Override
            protected void update(long tick) {
                double y = Easing.EASE_OUT_QUAD.between(0, riseHeight, progress());
                line.teleport(start.clone().add(0, y, 0));
            }

            @Override
            protected void onStop() {
                line.remove();
            }
        }.duration(ticks).start();
    }

    /** Indicador de dano en rojo. */
    public static Animation damage(Location at, double amount) {
        return spawn(at, "&c-" + format(amount), 24, 1.0);
    }

    /** Indicador de curacion en verde. */
    public static Animation heal(Location at, double amount) {
        return spawn(at, "&a+" + format(amount), 24, 1.0);
    }

    /** Indicador critico, mas grande y mas lento. */
    public static Animation critical(Location at, double amount) {
        return spawn(at, "&6&l✦ " + format(amount), 32, 1.4);
    }

    private static String format(double amount) {
        return amount == Math.floor(amount)
                ? String.valueOf((int) amount)
                : String.format("%.1f", amount);
    }

    private static double rand(double amount) {
        return (ThreadLocalRandom.current().nextDouble() * 2 - 1) * amount;
    }
}
