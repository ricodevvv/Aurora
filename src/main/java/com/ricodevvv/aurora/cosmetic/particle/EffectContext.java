package com.ricodevvv.aurora.cosmetic.particle;

import com.ricodevvv.aurora.particle.ParticleBuilder;
import com.ricodevvv.aurora.particle.RenderSettings;
import com.ricodevvv.aurora.shape.Shape;
import com.ricodevvv.aurora.util.ColorRamp;
import com.ricodevvv.aurora.util.Noise;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Everything a renderer needs to draw one frame of an effect.
 *
 * <p>The old renderer signature was {@code (player, location, tick)}, and that
 * is exactly enough to build an effect that looks free. Three things are
 * missing from it, and all three are what a paid cosmetic plugin gets right:
 *
 * <ol>
 *   <li><b>Continuous time.</b> {@link #time()} is measured in seconds, not in
 *       frames. An effect written against it rotates at the same speed whether
 *       it is drawn every tick or every fourth tick, which is what lets Aurora
 *       drop frames under load without the animation visibly slowing down.</li>
 *   <li><b>Motion.</b> {@link #state()}, {@link #velocity()} and
 *       {@link #trail(int, PathStep)} let an effect follow its wearer instead of
 *       teleporting after them. A trail drawn once per frame at a sprint leaves
 *       gaps a metre wide; the same trail interpolated over the distance
 *       actually travelled is continuous.</li>
 *   <li><b>Identity.</b> {@link #seed()} gives every wearer their own offset
 *       into the noise field, so ten players standing together stop pulsing in
 *       lockstep — the clearest single giveaway of a cheap effect.</li>
 * </ol>
 *
 * <p>The instance is reused between frames and belongs to the effect that owns
 * it. Do not hold on to it, and do not hold on to the {@link Location} it
 * hands out beyond the frame.
 */
public final class EffectContext {

    /** Rate at which the drawn body yaw catches up with the real one, per tick. */
    private static final double YAW_SMOOTHING = 0.25;

    private final Player player;
    private final double seed;

    private Location origin;
    private Location previous;
    private final Vector velocity = new Vector();

    private MovementState state = MovementState.IDLE;
    private long tick;
    private int delta = 1;
    private double bodyYaw;
    private List<Player> viewers = Collections.emptyList();

    /**
     * @param player the wearer
     */
    EffectContext(Player player) {
        this.player = player;
        this.seed = Noise.seed(player.getUniqueId());
        this.bodyYaw = player.getLocation().getYaw();
    }

    // ------------------------------------------------------------ frame setup

    /**
     * Advances the context to a new frame. Called by {@link ParticleEffect}.
     *
     * @param current current position of the wearer
     * @param tick    ticks since the effect was equipped
     * @param delta   ticks since the previous frame
     * @param state   what the wearer is doing
     * @param viewers who can see this frame
     */
    void update(Location current, long tick, int delta, MovementState state, List<Player> viewers) {
        this.previous = this.origin;
        this.origin = current;
        this.tick = tick;
        this.delta = Math.max(1, delta);
        this.state = state;
        this.viewers = viewers;

        if (previous != null && previous.getWorld() == current.getWorld()) {
            velocity.setX((current.getX() - previous.getX()) / this.delta);
            velocity.setY((current.getY() - previous.getY()) / this.delta);
            velocity.setZ((current.getZ() - previous.getZ()) / this.delta);
        } else {
            velocity.setX(0).setY(0).setZ(0);
            previous = null;
        }

        // Yaw is chased rather than copied. A player can spin 180 degrees in a
        // tick; wings that follow that exactly look like they are attached to a
        // turntable, and smoothing costs one lerp.
        double difference = wrapDegrees(current.getYaw() - bodyYaw);
        bodyYaw = wrapDegrees(bodyYaw + difference * Math.min(1, YAW_SMOOTHING * this.delta));
    }

    /**
     * Forgets the previous frame, so the next one does not interpolate across a
     * teleport or a world change.
     */
    void discontinue() {
        previous = null;
        origin = null;
        velocity.setX(0).setY(0).setZ(0);
    }

    // ----------------------------------------------------------------- state

    /**
     * @return the player wearing the effect
     */
    public Player player() {
        return player;
    }

    /**
     * @return the wearer's position this frame, at foot level; safe to mutate,
     * it is already a copy
     */
    public Location origin() {
        return origin;
    }

    /**
     * @return the wearer's position at the previous frame, or {@code null} on
     * the first frame and after a teleport
     */
    public Location previous() {
        return previous;
    }

    /**
     * @return ticks since the effect was equipped
     */
    public long tick() {
        return tick;
    }

    /**
     * Time since the effect was equipped, in seconds.
     *
     * <p>Drive every animation from this rather than from {@link #tick()}.
     * Speeds then read as "half a turn per second" and stay correct when the
     * effect's frame rate changes underneath them.
     *
     * @return elapsed seconds
     */
    public double time() {
        return tick / 20.0;
    }

    /**
     * @return ticks since the previous frame; {@code 1} at full frame rate
     */
    public int delta() {
        return delta;
    }

    /**
     * @return what the wearer is doing this frame
     */
    public MovementState state() {
        return state;
    }

    /**
     * @return {@code true} while the wearer is going somewhere
     */
    public boolean moving() {
        return state.moving();
    }

    /**
     * @return the wearer's displacement per tick, in blocks
     */
    public Vector velocity() {
        return velocity;
    }

    /**
     * @return horizontal speed in blocks per tick; roughly {@code 0.13} walking
     * and {@code 0.28} sprinting
     */
    public double speed() {
        double x = velocity.getX();
        double z = velocity.getZ();
        return Math.sqrt(x * x + z * z);
    }

    /**
     * The wearer's facing, smoothed over several ticks.
     *
     * <p>Use this and not {@code origin().getYaw()} for anything mounted on the
     * body — wings, capes, back-mounted shapes — or a flick of the mouse will
     * teleport it.
     *
     * @return the smoothed yaw, in degrees
     */
    public double yaw() {
        return bodyYaw;
    }

    /**
     * @return the smoothed yaw, in radians, ready for
     * {@link Shape#rotateY(double)}
     */
    public double yawRadians() {
        return Math.toRadians(-bodyYaw);
    }

    /**
     * @return a horizontal unit vector pointing where the wearer is facing
     */
    public Vector forward() {
        double radians = Math.toRadians(bodyYaw);
        return new Vector(-Math.sin(radians), 0, Math.cos(radians));
    }

    /**
     * @return a horizontal unit vector pointing to the wearer's right
     */
    public Vector right() {
        // forward x up, with Minecraft's axes: yaw 0 faces +Z (south), whose
        // right hand points at -X (west).
        double radians = Math.toRadians(bodyYaw);
        return new Vector(-Math.cos(radians), 0, -Math.sin(radians));
    }

    /**
     * A stable per-wearer offset into the noise field.
     *
     * @return the seed, constant for as long as the player is online
     */
    public double seed() {
        return seed;
    }

    /**
     * The wearer's own place in a shared cycle, so two players never pulse
     * together.
     *
     * @return a value in {@code 0..1}, constant per wearer
     */
    public double phase() {
        return (seed % 1.0 + 1.0) % 1.0;
    }

    /**
     * Smooth per-wearer noise that advances with time.
     *
     * @param frequency how fast it wanders; {@code 1} is roughly one swing a
     *                  second
     * @return a value in {@code -1..1}
     */
    public double noise(double frequency) {
        return Noise.at(time() * frequency + seed);
    }

    /**
     * Smooth noise that varies along a second axis as well as with time, for
     * per-point flicker across a shape.
     *
     * @param frequency how fast it wanders
     * @param axis      the second axis, usually a point index
     * @return a value in {@code -1..1}
     */
    public double noise(double frequency, double axis) {
        return Noise.at(time() * frequency + seed, axis);
    }

    /**
     * @return a random value in {@code 0..1}
     */
    public double random() {
        return ThreadLocalRandom.current().nextDouble();
    }

    /**
     * @return the players who can see this frame; resolved once for the whole
     * frame rather than per particle
     */
    public List<Player> viewers() {
        return viewers;
    }

    /**
     * Scales a procedurally chosen particle count by the current quality level.
     *
     * <p>Use it wherever an effect writes {@code for (int i = 0; i < 6; i++)}:
     * shapes already follow quality on their own, loose loops do not.
     *
     * @param count the count you would use at full quality
     * @return the count to actually use, never below {@code 1}
     */
    public int count(int count) {
        return Math.max(1, (int) Math.round(count * RenderSettings.density()));
    }

    // ---------------------------------------------------------------- anchors

    /**
     * A position relative to the wearer's feet.
     *
     * @param x offset along X
     * @param y offset along Y
     * @param z offset along Z
     * @return a new location
     */
    public Location at(double x, double y, double z) {
        return origin.clone().add(x, y, z);
    }

    /**
     * @param height height above the wearer's feet
     * @return a new location on the wearer's vertical axis
     */
    public Location above(double height) {
        return at(0, height, 0);
    }

    /**
     * @return roughly chest height, where most auras want to sit
     */
    public Location chest() {
        return at(0, 1.1, 0);
    }

    /**
     * @return roughly head height
     */
    public Location head() {
        return at(0, 1.9, 0);
    }

    /**
     * @return just above the ground, where circles and runes want to sit
     */
    public Location feet() {
        return at(0, 0.08, 0);
    }

    /**
     * A position behind the wearer, on their back rather than in world space.
     *
     * @param distance how far back, in blocks
     * @param height   height above the feet
     * @return a new location
     */
    public Location behind(double distance, double height) {
        Vector back = forward().multiply(-distance);
        return at(back.getX(), height, back.getZ());
    }

    /**
     * A position to one side of the wearer, mirrored by the sign of
     * {@code distance}.
     *
     * @param distance how far out; negative goes left
     * @param height   height above the feet
     * @return a new location
     */
    public Location beside(double distance, double height) {
        Vector side = right().multiply(distance);
        return at(side.getX(), height, side.getZ());
    }

    // ----------------------------------------------------------------- output

    /**
     * Spawns a particle at a location, using this frame's audience.
     *
     * @param particle particle to spawn
     * @param location where to spawn it
     */
    public void emit(ParticleBuilder particle, Location location) {
        particle.audience(viewers).spawn(location);
    }

    /**
     * Spawns a particle at an offset from the wearer's feet, without allocating
     * a {@link Location} for it. This is the cheapest way to draw single
     * particles and the one to reach for inside a loop.
     *
     * @param particle particle to spawn
     * @param x        offset along X
     * @param y        offset along Y
     * @param z        offset along Z
     */
    public void emit(ParticleBuilder particle, double x, double y, double z) {
        particle.audience(viewers).spawn(origin, x, y, z);
    }

    /**
     * Renders a shape around a location, using this frame's audience.
     *
     * @param particle particle to draw it with
     * @param location centre of the shape
     * @param shape    shape to render
     */
    public void emit(ParticleBuilder particle, Location location, Shape shape) {
        particle.audience(viewers).spawn(location, shape);
    }

    /**
     * Renders a shape with a colour sampled along its length.
     *
     * @param particle particle to draw it with
     * @param location centre of the shape
     * @param shape    shape to render
     * @param ramp     colour ramp
     */
    public void emit(ParticleBuilder particle, Location location, Shape shape, ColorRamp ramp) {
        particle.audience(viewers).spawn(location, shape, ramp);
    }

    /**
     * Walks the path the wearer actually covered since the previous frame.
     *
     * <p>This is what makes a trail continuous. A player sprinting covers about
     * 0.28 blocks a tick, so an effect drawn every third tick leaves a dot
     * every 0.84 blocks — visibly a dotted line. Emitting the same particle at
     * three points along the segment costs the same particles and reads as a
     * ribbon.
     *
     * <p>On the first frame, after a teleport, or when the wearer has not
     * moved, the step runs once at the current position.
     *
     * @param steps how many points along the path, at least one
     * @param step  called for each point, with its position and how far along
     *              the path it is
     */
    public void trail(int steps, PathStep step) {
        int total = Math.max(1, steps);
        if (previous == null || total == 1) {
            step.at(origin.clone(), 1);
            return;
        }

        double dx = origin.getX() - previous.getX();
        double dy = origin.getY() - previous.getY();
        double dz = origin.getZ() - previous.getZ();

        for (int i = 1; i <= total; i++) {
            double t = i / (double) total;
            Location position = origin.clone();
            position.setX(previous.getX() + dx * t);
            position.setY(previous.getY() + dy * t);
            position.setZ(previous.getZ() + dz * t);
            step.at(position, t);
        }
    }

    /**
     * One point along the wearer's path.
     *
     * @see #trail(int, PathStep)
     */
    @FunctionalInterface
    public interface PathStep {

        /**
         * @param position where this point is
         * @param progress how far along the segment it is, in {@code 0..1},
         *                 where {@code 1} is the wearer's current position
         */
        void at(Location position, double progress);
    }

    /**
     * Normalises an angle to {@code -180..180}, so smoothing across the wrap
     * point takes the short way round.
     *
     * @param degrees any angle
     * @return the equivalent angle in range
     */
    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360;
        if (wrapped >= 180) wrapped -= 360;
        if (wrapped < -180) wrapped += 360;
        return wrapped;
    }
}
