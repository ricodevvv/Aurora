package com.ricodevvv.aurora.cosmetic.balloon;

import com.ricodevvv.aurora.cosmetic.Cosmetic;
import com.cryptomorin.xseries.particles.XParticle;
import com.ricodevvv.aurora.particle.Particles;
import com.ricodevvv.aurora.shape.Shapes;
import com.ricodevvv.aurora.util.Entities;
import com.ricodevvv.aurora.util.Sounds;
import com.ricodevvv.aurora.util.VectorMath;
import org.bukkit.Location;
import com.ricodevvv.aurora.model.Model;
import com.ricodevvv.aurora.model.Models;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A balloon floating on a lead beside the player.
 *
 * <p>The balloon is simulated as a damped mass pushed by a noisy sinusoidal
 * wind and held by a hard distance constraint to an anchor at the player's
 * side. Each tick runs: decay inertia, add wind, move, clamp to the lead
 * length, then recompute inertia from the movement that actually happened.
 *
 * <p>That last step is the one that matters. Recomputing inertia from the
 * post-constraint displacement is what makes the balloon rebound when the lead
 * goes taut; without it the balloon simply pins itself to the end of the lead
 * and stops reacting.
 *
 * <p>Rendered as an armour stand wearing a helmet rather than an item display.
 * Displays only exist from 1.19.4 and {@code Attribute.SCALE} from 1.20.5,
 * whereas a helmeted armour stand behaves identically all the way back to 1.8.
 * That keeps this to one code path instead of three.
 */
public class Balloon extends Cosmetic {

    /** Inertia retained per tick. */
    private static final double INERTIA_DECAY = 0.95;
    /** Buoyancy floor: vertical inertia never drops below this. */
    private static final double LIFT = 0.1;
    /** Initial vertical inertia, so the balloon rises immediately on equip. */
    private static final double INITIAL_LIFT = 0.2;
    private static final double WIND_STRENGTH = 0.01;
    private static final double TURBULENCE = 0.08;
    /** Spin retained per tick. */
    private static final float ROTATION_DECAY = 0.98f;
    /** The balloon never stops spinning entirely. */
    private static final float MIN_ROTATION_SPEED = 1.0f;
    /** How much lateral movement is converted into spin. */
    private static final float ROTATION_MULTIPLIER = 1.0f;

    /**
     * Anchor offset from the player, before being rotated by their yaw.
     *
     * <p>The Y component is zero on purpose: the anchor sits at foot level, not
     * above the head. The balloon rises on its own buoyancy until the lead
     * stops it. Anchoring it high leaves it almost motionless, because it
     * starts already at the lead's limit with no travel left.
     */
    private static final Vector ANCHOR_OFFSET = new Vector(-0.8, 0.0, 0.2);

    private final BalloonType balloonType;

    private Model model;
    private Entity leashAnchor;

    private final Location position = new Location(null, 0, 0, 0);
    private final Vector inertia = new Vector(0, INITIAL_LIFT, 0);
    private final Vector wind = new Vector(1, 0, 0.3);

    private double windPhase = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
    private float spin = ThreadLocalRandom.current().nextFloat();
    private double angle;

    public Balloon(Player player, BalloonType type) {
        super(player, type);
        this.balloonType = type;
    }

    @Override
    protected void onEquip() {
        Location anchor = anchor();
        position.setWorld(anchor.getWorld());
        position.setX(anchor.getX());
        position.setY(anchor.getY());
        position.setZ(anchor.getZ());

        model = Models.armorStand(balloonType.small());
        model.spawn(anchor);
        model.helmet(balloonType.model());

        if (balloonType.leashMode() == BalloonType.LeashMode.ENTITY) {
            createLeashAnchor(anchor);
        }
        Sounds.POP.play(player, 0.6f, 1.8f);
    }

    /**
     * Creates the lead anchor: an invisible baby rabbit.
     *
     * <p>On 1.8 leads only attach to {@code Creature}, so neither an armour
     * stand nor a display entity can be leashed. The rabbit carries no AI and
     * is teleported to the balloon's position every tick, so the lead appears
     * to come from the balloon itself.
     *
     * <p>It is tagged, hidden and protected before anything else, because a
     * visible or killable anchor is the classic failure mode of this technique.
     *
     * @param at initial position
     */
    private void createLeashAnchor(Location at) {
        try {
            leashAnchor = at.getWorld().spawnEntity(at, Entities.type("RABBIT"));
            // Order matters: tag first, so the protection listener recognises
            // the entity from its very first tick.
            Entities.tag(leashAnchor);
            Entities.baby(leashAnchor);
            Entities.invisible(leashAnchor);
            Entities.disableAI(leashAnchor);
            Entities.silence(leashAnchor);
            Entities.invulnerable(leashAnchor);
            Entities.collidable(leashAnchor, false);
            Entities.persist(leashAnchor);

            if (!Entities.leashTo(leashAnchor, player)) {
                leashAnchor.remove();
                leashAnchor = null;
            }
        } catch (Throwable t) {
            leashAnchor = null;
        }
    }

    @Override
    protected void onTick(long tick) {
        Location anchor = anchor();
        if (anchor.getWorld() == null) return;

        // World change: reposition instantly rather than interpolating across worlds.
        if (position.getWorld() != anchor.getWorld()) {
            teleportTo(anchor);
            return;
        }

        Vector previous = position.toVector();

        // Damped inertia plus buoyancy.
        inertia.multiply(INERTIA_DECAY);
        if (inertia.getY() < LIFT) inertia.setY(Math.min(LIFT, inertia.getY() + LIFT));

        // Sinusoidal wind plus random turbulence.
        windPhase += TURBULENCE;
        double magnitude = Math.sin(windPhase) * WIND_STRENGTH;
        inertia.add(new Vector(
                wind.getX() * magnitude + noise(),
                wind.getY() * magnitude + noise(),
                wind.getZ() * magnitude + noise()));

        position.add(inertia.getX(), inertia.getY(), inertia.getZ());

        // Hard lead-length constraint.
        double distance = position.distance(anchor);
        if (distance > balloonType.leashLength()) {
            double ratio = balloonType.leashLength() / distance;
            position.setX(anchor.getX() + (position.getX() - anchor.getX()) * ratio);
            position.setY(anchor.getY() + (position.getY() - anchor.getY()) * ratio);
            position.setZ(anchor.getZ() + (position.getZ() - anchor.getZ()) * ratio);
        }

        // Real inertia is the displacement that survived the constraint.
        // Without this the balloon never rebounds when the lead goes taut.
        inertia.setX(position.getX() - previous.getX());
        inertia.setY(position.getY() - previous.getY());
        inertia.setZ(position.getZ() - previous.getZ());

        // Spin is fed by lateral movement, with a floor so the balloon never
        // comes to a complete stop.
        spin *= ROTATION_DECAY;
        if (Math.abs(spin) < MIN_ROTATION_SPEED) {
            spin = spin >= 0 ? MIN_ROTATION_SPEED : -MIN_ROTATION_SPEED;
        }
        angle += spin;
        spin += (float) ((inertia.getX() - inertia.getZ()) * ROTATION_MULTIPLIER);
        position.setYaw((float) angle);

        model.teleport(position);
        model.headYaw(position.getYaw());
        if (leashAnchor != null && leashAnchor.isValid()) {
            leashAnchor.teleport(position);
        }

        if (balloonType.leashMode() == BalloonType.LeashMode.PARTICLE) {
            drawParticleLeash(anchor);
        }
        // Viewer tracking only applies to the packet backend, and teleport
        // already refreshes it, so nothing extra is needed here.
        if (balloonType.ambientParticle() != null && tick % balloonType.ambientInterval() == 0) {
            balloonType.ambientParticle().spawn(position.clone().add(0, 0.6, 0));
        }
    }

    /**
     * Draws the lead with particles instead of a real lead.
     *
     * @param anchor where the lead starts
     */
    private void drawParticleLeash(Location anchor) {
        Vector delta = position.toVector().subtract(anchor.toVector());
        Particles.of(XParticle.SMOKE).count(1).speed(0)
                .spawn(anchor, Shapes.line(new Vector(0, 0, 0), delta, 0.25));
    }

    /**
     * Computes the anchor position: the player's side, rotated by their yaw.
     *
     * @return where the lead is held this tick
     */
    private Location anchor() {
        Location base = player.getLocation();
        Vector offset = ANCHOR_OFFSET.clone();
        VectorMath.rotateY(offset, Math.toRadians(-base.getYaw()));
        return base.clone().add(offset.getX(),
                offset.getY() + balloonType.height(), offset.getZ());
    }

    /**
     * Snaps the balloon to a position and resets its motion, used on world
     * changes where interpolating would be meaningless.
     *
     * @param location position to snap to
     */
    private void teleportTo(Location location) {
        position.setWorld(location.getWorld());
        position.setX(location.getX());
        position.setY(location.getY());
        position.setZ(location.getZ());
        inertia.setX(0);
        inertia.setY(INITIAL_LIFT);
        inertia.setZ(0);
        if (model != null && model.alive()) model.teleport(position);
        if (leashAnchor != null && leashAnchor.isValid()) leashAnchor.teleport(position);
    }

    @Override
    protected void onUnequip() {
        if (model != null && model.alive()) {
            // Pop: a ten-particle cloud with 0.15 spread at 0.05 speed.
            Location at = position.clone().add(0, 0.5, 0);
            Particles.of(XParticle.CLOUD).count(10).offset(0.15).speed(0.05).spawn(at);
            Particles.dust(balloonType.popColor()).count(1).spawn(at, Shapes.sphere(0.35, 10));
            Sounds.POP.playAt(at, 0.5f, 0.5f);
            model.destroy();
        }
        if (leashAnchor != null && leashAnchor.isValid()) leashAnchor.remove();
        model = null;
        leashAnchor = null;
    }

    /**
     * @return the balloon's visible model, or {@code null} if not equipped
     */
    public Model model() {
        return model;
    }

    private static double noise() {
        return (ThreadLocalRandom.current().nextDouble() - 0.5) * WIND_STRENGTH;
    }
}
