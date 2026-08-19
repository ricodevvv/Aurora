package com.ricodevvv.aurora.cosmetic.particle;

import com.ricodevvv.aurora.cosmetic.Cosmetic;
import com.ricodevvv.aurora.particle.RenderSettings;
import com.ricodevvv.aurora.particle.ViewerCache;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * A particle effect worn by a player.
 *
 * <p>This is the frame pipeline, and it is where the cost of a cosmetic system
 * is decided. In order, every frame:
 *
 * <ol>
 *   <li>skips entirely unless the frame is due, which depends on the effect's
 *       interval stretched by the current {@link RenderSettings} quality;</li>
 *   <li>skips if the wearer should not be drawn at all — spectator, invisible,
 *       vanished, or the whole renderer switched off;</li>
 *   <li>resolves the audience once, through the shared per-tick
 *       {@link ViewerCache}, and skips if nobody is close enough;</li>
 *   <li>classifies what the wearer is doing and hands the matching variant a
 *       fully populated {@link EffectContext}.</li>
 * </ol>
 *
 * <p>Nothing is allocated for a frame that is not going to be seen, which with
 * fifty wearers in a lobby is the difference between noticeable and not.
 */
public class ParticleEffect extends Cosmetic {

    /** Horizontal speed, in blocks per tick, above which the wearer counts as walking. */
    private static final double WALK_THRESHOLD = 0.02;

    /** Squared horizontal jump, in blocks, treated as a teleport rather than movement. */
    private static final double TELEPORT_SQ = 64;

    private final ParticleEffectType effectType;
    private final EffectContext context;

    private Location previous;
    private long lastFrame = Long.MIN_VALUE;
    private MovementState state = MovementState.IDLE;

    public ParticleEffect(Player player, ParticleEffectType type) {
        super(player, type);
        this.effectType = type;
        this.context = new EffectContext(player);
    }

    @Override
    protected void onEquip() {
        previous = player.getLocation();
    }

    @Override
    protected void onTick(long tick) {
        int interval = RenderSettings.interval(effectType.interval());
        if (lastFrame != Long.MIN_VALUE && tick - lastFrame < interval) return;

        Location current = player.getLocation();
        state = classify(current);

        if (skip(current)) {
            // Keep tracking position while hidden, so reappearing does not draw
            // a trail across half the map.
            previous = current;
            context.discontinue();
            return;
        }

        List<Player> audience = audience(current);
        if (audience.isEmpty()) {
            previous = current;
            context.discontinue();
            return;
        }

        int delta = lastFrame == Long.MIN_VALUE ? interval : (int) (tick - lastFrame);
        lastFrame = tick;
        previous = current;

        context.update(current, tick, delta, state, audience);

        try {
            effectType.renderer(state).render(context);
            EffectSound sound = effectType.soundSpec();
            if (sound != null) sound.play(tick, current, audience);
        } catch (Throwable thrown) {
            Bukkit.getLogger().warning(
                    "[Aurora] Effect " + effectType.id() + " failed and was removed: " + thrown);
            stop();
        }
    }

    /**
     * Works out what the wearer is doing, from cheapest test to most expensive.
     *
     * @param current the wearer's position this frame
     * @return the movement state
     */
    private MovementState classify(Location current) {
        if (player.isFlying()) return MovementState.FLYING;

        double speed = horizontalSpeed(current);
        if (!player.isOnGround()) return MovementState.AIRBORNE;
        if (player.isSneaking()) return MovementState.SNEAKING;
        if (speed <= WALK_THRESHOLD) return MovementState.IDLE;
        return player.isSprinting() ? MovementState.SPRINTING : MovementState.WALKING;
    }

    /**
     * Horizontal distance covered since the previous frame, per tick.
     *
     * @param current the wearer's position this frame
     * @return blocks per tick, or {@code 0} across a teleport or world change
     */
    private double horizontalSpeed(Location current) {
        if (previous == null || previous.getWorld() != current.getWorld()) return 0;

        double dx = current.getX() - previous.getX();
        double dz = current.getZ() - previous.getZ();
        double squared = dx * dx + dz * dz;
        if (squared > TELEPORT_SQ) return 0;

        int interval = Math.max(1, RenderSettings.interval(effectType.interval()));
        return Math.sqrt(squared) / interval;
    }

    /**
     * Decides whether this frame can be skipped before anything is built.
     *
     * @param at the wearer's position
     * @return {@code true} if nothing should be drawn
     */
    private boolean skip(Location at) {
        if (!RenderSettings.enabled()) return true;
        if (at.getWorld() == null) return true;
        if (player.getGameMode() == GameMode.SPECTATOR) return true;
        return effectType.hideWhenInvisible() && hidden(player);
    }

    /**
     * Whether a player is currently hidden from other players.
     *
     * <p>An effect that keeps drawing on an invisible or vanished player turns
     * a cosmetic into a wallhack, so both are checked: the potion effect, and
     * the {@code vanished} metadata key that every vanish plugin in the
     * ecosystem sets.
     *
     * @param player player to test
     * @return {@code true} if they should not be drawn
     */
    private static boolean hidden(Player player) {
        try {
            if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) return true;
        } catch (Throwable ignored) {
            // Potion type lookup differs across versions; treat it as visible.
        }
        for (MetadataValue value : player.getMetadata("vanished")) {
            if (value.asBoolean()) return true;
        }
        return false;
    }

    /**
     * Resolves who should see this frame.
     *
     * @param at the wearer's position
     * @return the audience, possibly empty
     */
    private List<Player> audience(Location at) {
        List<Player> nearby = ViewerCache.near(at, RenderSettings.range(effectType.range()));
        if (effectType.showToWearer() || nearby.isEmpty()) return nearby;

        List<Player> filtered = new ArrayList<>(nearby.size());
        for (int i = 0; i < nearby.size(); i++) {
            Player viewer = nearby.get(i);
            if (viewer != player) filtered.add(viewer);
        }
        return filtered;
    }

    @Override
    protected void onUnequip() {
        previous = null;
        lastFrame = Long.MIN_VALUE;
        context.discontinue();
    }

    /**
     * @return {@code true} while the wearer is going somewhere
     */
    public boolean isMoving() {
        return state.moving();
    }

    /**
     * @return what the wearer was doing on the last drawn frame
     */
    public MovementState state() {
        return state;
    }
}
