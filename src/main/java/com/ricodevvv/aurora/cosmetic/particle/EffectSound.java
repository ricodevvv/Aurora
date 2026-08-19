package com.ricodevvv.aurora.cosmetic.particle;

import com.ricodevvv.aurora.util.Sounds;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The quiet loop that plays under an effect.
 *
 * <p>Sound is the half of "premium" that costs nothing and that free plugins
 * always skip. A flame aura with a soft crackle every second reads as a fire; a
 * silent one reads as orange dots. The rules that keep it from becoming
 * annoying are the same ones the particles follow: it goes only to players who
 * can already see the effect, it is quiet, and its pitch varies so repeats do
 * not sound mechanical.
 */
public final class EffectSound {

    private final Sounds sound;
    private final int interval;
    private final float volume;
    private final float minPitch;
    private final float maxPitch;

    /**
     * @param sound    which sound to play
     * @param interval ticks between plays
     * @param volume   volume, which doubles as the falloff radius in Minecraft;
     *                 keep it well below {@code 1}
     * @param minPitch lowest pitch
     * @param maxPitch highest pitch; equal to {@code minPitch} for a fixed note
     */
    public EffectSound(Sounds sound, int interval, float volume, float minPitch, float maxPitch) {
        this.sound = sound;
        this.interval = Math.max(1, interval);
        this.volume = volume;
        this.minPitch = minPitch;
        this.maxPitch = Math.max(minPitch, maxPitch);
    }

    /**
     * A sound at a fixed pitch.
     *
     * @param sound    which sound to play
     * @param interval ticks between plays
     * @param volume   volume
     * @param pitch    pitch
     */
    public EffectSound(Sounds sound, int interval, float volume, float pitch) {
        this(sound, interval, volume, pitch, pitch);
    }

    /**
     * Plays the sound if this tick is due, to the players who can see the
     * effect and to nobody else.
     *
     * @param tick     ticks since the effect was equipped
     * @param at       where the sound comes from
     * @param audience players who can see the effect
     */
    void play(long tick, Location at, List<Player> audience) {
        if (tick % interval != 0 || audience.isEmpty()) return;

        float pitch = minPitch == maxPitch ? minPitch
                : (float) ThreadLocalRandom.current().nextDouble(minPitch, maxPitch);

        for (int i = 0; i < audience.size(); i++) {
            sound.play(audience.get(i), at, volume, pitch);
        }
    }

    /**
     * @return ticks between plays
     */
    public int interval() {
        return interval;
    }
}
