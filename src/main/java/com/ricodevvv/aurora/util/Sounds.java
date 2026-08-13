package com.ricodevvv.aurora.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Cross-version sound keys, from 1.8 through 1.21 and later.
 *
 * <p>The {@code org.bukkit.Sound} enum is deliberately avoided. Its constant
 * names were rewritten wholesale in 1.9 ({@code LEVEL_UP} became
 * {@code ENTITY_PLAYER_LEVELUP}), again in 1.13 ({@code BLOCK_NOTE_PLING}
 * became {@code BLOCK_NOTE_BLOCK_PLING}), and in 1.21.3 {@code Sound} stopped
 * being an enum at all and became a registry. Any library that references the
 * enum breaks at one of those points.
 *
 * <p>What has never changed is
 * {@code Player#playSound(Location, String, float, float)}, present since 1.8,
 * so every sound here is stored as three raw keys and the right one is picked
 * at runtime.
 */
public enum Sounds {

    /** UI click. */
    CLICK("random.click", "ui.button.click", "ui.button.click"),
    /** Level-up chime. */
    LEVEL_UP("random.levelup", "entity.player.levelup", "entity.player.levelup"),
    /** Experience orb pickup. */
    ORB("random.orb", "entity.experience_orb.pickup", "entity.experience_orb.pickup"),
    /** Item pickup pop. */
    POP("random.pop", "entity.item.pickup", "entity.item.pickup"),
    /** Generic explosion. */
    EXPLODE("random.explode", "entity.generic.explode", "entity.generic.explode"),
    /** Anvil landing thud. */
    ANVIL_LAND("random.anvil_land", "block.anvil.land", "block.anvil.land"),
    /** Note block pling. */
    NOTE_PLING("note.pling", "block.note.pling", "block.note_block.pling"),
    /** Note block harp. */
    NOTE_HARP("note.harp", "block.note.harp", "block.note_block.harp"),
    /** Firework detonation. */
    FIREWORK_BLAST("fireworks.blast", "entity.firework.blast", "entity.firework_rocket.blast"),
    /** Firework launch. */
    FIREWORK_LAUNCH("fireworks.launch", "entity.firework.launch", "entity.firework_rocket.launch"),
    /** Ender dragon growl. */
    DRAGON_GROWL("mob.enderdragon.growl", "entity.enderdragon.growl", "entity.ender_dragon.growl"),
    /** Enderman teleport. */
    ENDERMAN_TELEPORT("mob.endermen.portal", "entity.endermen.teleport", "entity.enderman.teleport"),
    /** Wither spawn roar. */
    WITHER_SPAWN("mob.wither.spawn", "entity.wither.spawn", "entity.wither.spawn"),
    /** Glass breaking. */
    GLASS_BREAK("dig.glass", "block.glass.break", "block.glass.break"),
    /** Flint and steel ignition. */
    FIRE_IGNITE("fire.ignite", "item.flintandsteel.use", "item.flintandsteel.use"),
    /** Bow release. */
    BOW_SHOOT("random.bow", "entity.arrow.shoot", "entity.arrow.shoot"),
    /** Arrow impact. */
    ARROW_HIT("random.bowhit", "entity.arrow.hit", "entity.arrow.hit"),
    /** Player hurt. */
    HURT("game.player.hurt", "entity.player.hurt", "entity.player.hurt"),
    /** Player burp. */
    BURP("random.burp", "entity.player.burp", "entity.player.burp"),
    /** Nether portal travel. */
    PORTAL_TRAVEL("portal.travel", "block.portal.travel", "block.portal.travel"),
    /** Chest opening. */
    CHEST_OPEN("random.chestopen", "block.chest.open", "block.chest.open");

    private final String legacy;
    private final String modern;
    private final String flattened;

    /**
     * @param legacy    key used on 1.8
     * @param modern    key used on 1.9 through 1.12
     * @param flattened key used on 1.13 and later
     */
    Sounds(String legacy, String modern, String flattened) {
        this.legacy = legacy;
        this.modern = modern;
        this.flattened = flattened;
    }

    /**
     * @return the sound key for the running server version
     */
    public String key() {
        if (ServerVersion.isLegacy()) return legacy;
        return ServerVersion.isFlattened() ? flattened : modern;
    }

    /**
     * Plays the sound to one player, at that player's position.
     *
     * @param player player who hears it
     * @param volume volume, {@code 1.0} being normal
     * @param pitch  pitch in {@code 0.5..2.0}
     */
    public void play(Player player, float volume, float pitch) {
        play(player, player.getLocation(), volume, pitch);
    }

    /**
     * Plays the sound to one player, originating from a position.
     *
     * @param player player who hears it
     * @param at     where the sound comes from
     * @param volume volume, {@code 1.0} being normal
     * @param pitch  pitch in {@code 0.5..2.0}
     */
    public void play(Player player, Location at, float volume, float pitch) {
        try {
            player.playSound(at, key(), volume, pitch);
        } catch (Throwable ignored) {
            // Unknown key on this version; silence is better than a stack trace.
        }
    }

    /**
     * Plays the sound to everyone in the world who is close enough to hear it.
     *
     * @param at     where the sound comes from
     * @param volume volume, {@code 1.0} being normal
     * @param pitch  pitch in {@code 0.5..2.0}
     */
    public void playAt(Location at, float volume, float pitch) {
        if (at.getWorld() == null) return;
        for (Player player : at.getWorld().getPlayers()) {
            play(player, at, volume, pitch);
        }
    }

    /**
     * Converts a musical interval to a Minecraft pitch value, for building
     * melodies without hardcoding floats.
     *
     * @param semitones semitones above the base note; may be negative
     * @return the pitch value, where {@code 0} semitones yields {@code 1.0}
     */
    public static float pitchOf(int semitones) {
        return (float) Math.pow(2, semitones / 12D);
    }
}
