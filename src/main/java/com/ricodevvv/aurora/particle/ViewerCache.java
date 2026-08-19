package com.ricodevvv.aurora.particle;

import com.ricodevvv.aurora.util.ServerLoad;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-tick snapshot of who is in each world, so effects stop rescanning.
 *
 * <p>{@code World#getPlayers()} copies its player list on every call. An effect
 * that spawns twelve shapes a frame used to pay for twelve copies of the whole
 * lobby's player list, per wearer, per frame — with fifty wearers on a
 * hundred-player server that is sixty thousand list copies a second before a
 * single particle exists.
 *
 * <p>The snapshot is taken at most once per world per tick and handed out
 * read-only. Players who join or leave mid-tick are picked up on the next one,
 * which is a tick of latency on a cosmetic and matters to nobody.
 */
public final class ViewerCache {

    private static final Map<UUID, List<Player>> WORLDS = new HashMap<>();

    /**
     * Scratch location reused by the distance tests. {@code Entity#getLocation()}
     * allocates a new {@link Location} on every call, and these tests run
     * against every player for every effect frame; the overload that fills an
     * existing location has been there since 1.8.
     */
    private static final Location SCRATCH = new Location(null, 0, 0, 0);

    private static long cachedTick = -1;

    private ViewerCache() {
    }

    /**
     * Drops the snapshot. Called once per tick by
     * {@link com.ricodevvv.aurora.animation.AnimationManager}.
     */
    public static void invalidate() {
        WORLDS.clear();
        cachedTick = ServerLoad.ticks();
    }

    /**
     * Every player in a world, cached for the current tick.
     *
     * @param world world to look in
     * @return an unmodifiable snapshot; empty if the world is {@code null}
     */
    public static List<Player> inWorld(World world) {
        if (world == null) return Collections.emptyList();

        long tick = ServerLoad.ticks();
        if (tick != cachedTick) {
            WORLDS.clear();
            cachedTick = tick;
        }
        return WORLDS.computeIfAbsent(world.getUID(),
                key -> Collections.unmodifiableList(new ArrayList<>(world.getPlayers())));
    }

    /**
     * Players close enough to a location to be sent particles.
     *
     * <p>Resolve this once per effect frame and hand the result to every
     * builder that frame, rather than letting each spawn call work it out
     * again.
     *
     * @param at    centre of the search
     * @param range radius in blocks
     * @return the players in range, in world order
     */
    public static List<Player> near(Location at, double range) {
        if (at == null || at.getWorld() == null) return Collections.emptyList();

        List<Player> candidates = inWorld(at.getWorld());
        if (candidates.isEmpty()) return Collections.emptyList();

        double rangeSq = range * range;
        List<Player> found = new ArrayList<>(Math.min(candidates.size(), 8));
        for (int i = 0; i < candidates.size(); i++) {
            Player player = candidates.get(i);
            if (withinSq(player, at, rangeSq)) found.add(player);
        }
        return found;
    }

    /**
     * Cheap "is anyone even watching" test that stops as soon as it finds one.
     *
     * @param at    centre of the search
     * @param range radius in blocks
     * @return {@code true} if at least one player is in range
     */
    public static boolean anyNear(Location at, double range) {
        if (at == null || at.getWorld() == null) return false;

        List<Player> candidates = inWorld(at.getWorld());
        double rangeSq = range * range;
        for (int i = 0; i < candidates.size(); i++) {
            if (withinSq(candidates.get(i), at, rangeSq)) return true;
        }
        return false;
    }

    /**
     * Squared distance test that avoids {@code Location#distanceSquared}, which
     * throws when the worlds differ and allocates a {@code Location} for the
     * player on every call.
     *
     * @param player  player to test
     * @param at      centre
     * @param rangeSq squared radius
     * @return {@code true} if the player is within range
     */
    private static boolean withinSq(Player player, Location at, double rangeSq) {
        Location location = player.getLocation(SCRATCH);
        if (location.getWorld() != at.getWorld()) return false;

        double dx = location.getX() - at.getX();
        double dy = location.getY() - at.getY();
        double dz = location.getZ() - at.getZ();
        return dx * dx + dy * dy + dz * dz <= rangeSq;
    }
}
