package com.ricodevvv.aurora.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Sonidos multiversion sin tocar el enum Sound.
 *
 * Por que no uso org.bukkit.Sound: los nombres cambiaron completo en 1.9
 * (LEVEL_UP -> ENTITY_PLAYER_LEVELUP), otra vez en 1.13 (BLOCK_NOTE_PLING ->
 * BLOCK_NOTE_BLOCK_PLING) y en 1.21.3 Sound dejo de ser enum para volverse
 * registro. En cambio la sobrecarga playSound(Location, String, float, float)
 * existe desde 1.8 y sigue igual, asi que mandamos la clave como String.
 */
public enum Sounds {

    CLICK("random.click", "ui.button.click", "ui.button.click"),
    LEVEL_UP("random.levelup", "entity.player.levelup", "entity.player.levelup"),
    ORB("random.orb", "entity.experience_orb.pickup", "entity.experience_orb.pickup"),
    POP("random.pop", "entity.item.pickup", "entity.item.pickup"),
    EXPLODE("random.explode", "entity.generic.explode", "entity.generic.explode"),
    ANVIL_LAND("random.anvil_land", "block.anvil.land", "block.anvil.land"),
    NOTE_PLING("note.pling", "block.note.pling", "block.note_block.pling"),
    NOTE_HARP("note.harp", "block.note.harp", "block.note_block.harp"),
    FIREWORK_BLAST("fireworks.blast", "entity.firework.blast", "entity.firework_rocket.blast"),
    FIREWORK_LAUNCH("fireworks.launch", "entity.firework.launch", "entity.firework_rocket.launch"),
    DRAGON_GROWL("mob.enderdragon.growl", "entity.enderdragon.growl", "entity.ender_dragon.growl"),
    ENDERMAN_TELEPORT("mob.endermen.portal", "entity.endermen.teleport", "entity.enderman.teleport"),
    WITHER_SPAWN("mob.wither.spawn", "entity.wither.spawn", "entity.wither.spawn"),
    GLASS_BREAK("dig.glass", "block.glass.break", "block.glass.break"),
    FIRE_IGNITE("fire.ignite", "item.flintandsteel.use", "item.flintandsteel.use"),
    BOW_SHOOT("random.bow", "entity.arrow.shoot", "entity.arrow.shoot"),
    ARROW_HIT("random.bowhit", "entity.arrow.hit", "entity.arrow.hit"),
    HURT("game.player.hurt", "entity.player.hurt", "entity.player.hurt"),
    BURP("random.burp", "entity.player.burp", "entity.player.burp"),
    PORTAL_TRAVEL("portal.travel", "block.portal.travel", "block.portal.travel"),
    CHEST_OPEN("random.chestopen", "block.chest.open", "block.chest.open");

    private final String legacy;    // 1.8
    private final String modern;    // 1.9 - 1.12
    private final String flattened; // 1.13+

    Sounds(String legacy, String modern, String flattened) {
        this.legacy = legacy;
        this.modern = modern;
        this.flattened = flattened;
    }

    public String key() {
        if (ServerVersion.isLegacy()) return legacy;
        return ServerVersion.isFlattened() ? flattened : modern;
    }

    public void play(Player player, float volume, float pitch) {
        play(player, player.getLocation(), volume, pitch);
    }

    public void play(Player player, Location at, float volume, float pitch) {
        try {
            player.playSound(at, key(), volume, pitch);
        } catch (Throwable ignored) {
        }
    }

    /** Lo escuchan todos los que esten cerca. */
    public void playAt(Location at, float volume, float pitch) {
        if (at.getWorld() == null) return;
        for (Player player : at.getWorld().getPlayers()) {
            play(player, at, volume, pitch);
        }
    }

    /**
     * Melodia rapida: notas como semitonos relativos.
     * El pitch de Minecraft va de 0.5 a 2.0, donde 1.0 es la nota base.
     */
    public static float pitchOf(int semitones) {
        return (float) Math.pow(2, semitones / 12D);
    }
}
