package com.ricodevvv.aurora.cosmetic.particle;

import com.ricodevvv.aurora.cosmetic.Cosmetic;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * A particle effect worn by a player.
 *
 * <p>Work is skipped before any particle is built: if nobody is in range, or
 * the wearer is in spectator mode, the frame costs nothing. With fifty players
 * wearing effects in a lobby that check is the difference between noticeable
 * and not.
 */
public class ParticleEffect extends Cosmetic {

    private final ParticleEffectType effectType;

    private Location previous;
    private boolean moving;

    public ParticleEffect(Player player, ParticleEffectType type) {
        super(player, type);
        this.effectType = type;
    }

    @Override
    protected void onEquip() {
        previous = player.getLocation();
    }

    @Override
    protected void onTick(long tick) {
        if (tick % effectType.interval() != 0) return;

        Location current = player.getLocation();
        updateMovement(current);

        if (skip(current)) return;

        try {
            effectType.renderer(moving).render(player, current.clone(), tick);
        } catch (Throwable t) {
            org.bukkit.Bukkit.getLogger().warning(
                    "[Aurora] Efecto " + effectType.id() + " reviento: " + t);
            stop();
        }
    }

    private void updateMovement(Location current) {
        if (previous != null && previous.getWorld() == current.getWorld()) {
            double dx = current.getX() - previous.getX();
            double dz = current.getZ() - previous.getZ();
            moving = (dx * dx + dz * dz) > 0.003;
        }
        previous = current;
    }

    /**
     * Decides whether this frame can be skipped entirely.
     *
     * @param at the wearer's position
     * @return {@code true} if nothing should be drawn
     */
    private boolean skip(Location at) {
        if (at.getWorld() == null) return true;
        if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return true;

        double rangeSq = effectType.range() * effectType.range();
        for (Player viewer : at.getWorld().getPlayers()) {
            if (viewer.getLocation().distanceSquared(at) <= rangeSq) return false;
        }
        return true;
    }

    @Override
    protected void onUnequip() {
        previous = null;
    }

    public boolean isMoving() {
        return moving;
    }
}
