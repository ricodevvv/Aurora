package com.ricodevvv.aurora.hologram;

import com.ricodevvv.aurora.animation.Animation;
import com.ricodevvv.aurora.util.Easing;
import org.bukkit.Location;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Single-use floating text that rises and removes itself: the damage indicator
 * familiar from practice servers.
 *
 * <p>Each call spawns and destroys an armour stand. That is fine for a duel,
 * but in a hundred-player event it adds up; pool the stands or rate-limit per
 * player before using it at that scale.
 */
public final class FloatingText {

    private FloatingText() {
    }

    /**
     * Spawns text that rises and fades out.
     *
     * @param at         where it appears
     * @param text       text to show, supporting {@code &} colour codes
     * @param ticks      lifetime in ticks
     * @param riseHeight how far it travels upward, in blocks
     * @return the running animation, already started
     */
    public static Animation spawn(Location at, String text, int ticks, double riseHeight) {
        TextLine line = new TextLine(text);
        // Scattered slightly so rapid hits do not stack into one blob.
        Location start = at.clone().add(rand(0.4), rand(0.2), rand(0.4));
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

    /**
     * Red damage indicator.
     *
     * @param at     where it appears
     * @param amount damage dealt
     * @return the running animation
     */
    public static Animation damage(Location at, double amount) {
        return spawn(at, "&c-" + format(amount), 24, 1.0);
    }

    /**
     * Green healing indicator.
     *
     * @param at     where it appears
     * @param amount health restored
     * @return the running animation
     */
    public static Animation heal(Location at, double amount) {
        return spawn(at, "&a+" + format(amount), 24, 1.0);
    }

    /**
     * Critical hit indicator: larger, slower and gold.
     *
     * @param at     where it appears
     * @param amount damage dealt
     * @return the running animation
     */
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
