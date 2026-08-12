package com.ricodevvv.aurora.cosmetic.balloon;

import com.ricodevvv.aurora.cosmetic.Cosmetic;
import com.ricodevvv.aurora.particle.ParticleType;
import com.ricodevvv.aurora.particle.Particles;
import com.ricodevvv.aurora.shape.Shapes;
import com.ricodevvv.aurora.util.Entities;
import com.ricodevvv.aurora.util.ServerVersion;
import com.ricodevvv.aurora.util.Sounds;
import org.bukkit.Location;
import com.ricodevvv.aurora.model.Model;
import com.ricodevvv.aurora.model.Models;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Globo que flota atado al jugador.
 *
 * Simulacion: el globo es una masa con inercia amortiguada, empujada por un
 * viento senoidal con algo de ruido, y sujeta por una restriccion dura de
 * distancia al punto de anclaje (el hombro del jugador). Cada tick:
 * inercia -> viento -> mover -> recortar a la longitud de cuerda ->
 * recalcular inercia desde el desplazamiento real. Esa ultima parte es lo que
 * hace que el globo "rebote" al final de la cuerda en vez de quedarse tieso.
 *
 * Por que ArmorStand y no ItemDisplay: ItemDisplay es 1.19.4+, y el escalado
 * por Attribute.SCALE es 1.20.5+. El ArmorStand con casco funciona identico
 * desde 1.8, asi que hay UN solo camino de codigo en vez de tres.
 */
public class Balloon extends Cosmetic {

    private static final double INERTIA_DECAY = 0.95;
    private static final double LIFT = 0.1;
    private static final double WIND_STRENGTH = 0.012;
    private static final double TURBULENCE = 0.08;
    private static final double SHOULDER_OFFSET = 0.7;

    private final BalloonType balloonType;

    private Model model;
    private Entity leashAnchor;

    private final Location position = new Location(null, 0, 0, 0);
    private final Vector inertia = new Vector(0, LIFT, 0);
    private final Vector wind = new Vector(1, 0, 0.3);

    private double windPhase = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
    private float spin = ThreadLocalRandom.current().nextFloat();

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
     * Ancla de la correa: un conejo bebe invisible.
     *
     * En 1.8 la correa SOLO funciona sobre Creature, asi que ni el ArmorStand
     * ni un Display sirven. El conejo va sin IA y se teletransporta cada tick
     * a la posicion del globo, asi que visualmente la cuerda sale del globo.
     */
    private void createLeashAnchor(Location at) {
        try {
            leashAnchor = at.getWorld().spawnEntity(at, Entities.type("RABBIT"));
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

        // Cambio de mundo: reposicionar en seco, sin interpolar.
        if (position.getWorld() != anchor.getWorld()) {
            teleportTo(anchor);
            return;
        }

        Vector previous = position.toVector();

        // Inercia amortiguada + flotabilidad
        inertia.multiply(INERTIA_DECAY);
        if (inertia.getY() < LIFT) inertia.setY(Math.min(LIFT, inertia.getY() + LIFT));

        // Viento senoidal + turbulencia
        windPhase += TURBULENCE;
        double magnitude = Math.sin(windPhase) * WIND_STRENGTH;
        inertia.add(new Vector(
                wind.getX() * magnitude + noise(),
                wind.getY() * magnitude + noise(),
                wind.getZ() * magnitude + noise()));

        position.add(inertia.getX(), inertia.getY(), inertia.getZ());

        // Restriccion dura de cuerda
        double distance = position.distance(anchor);
        if (distance > balloonType.leashLength()) {
            double ratio = balloonType.leashLength() / distance;
            position.setX(anchor.getX() + (position.getX() - anchor.getX()) * ratio);
            position.setY(anchor.getY() + (position.getY() - anchor.getY()) * ratio);
            position.setZ(anchor.getZ() + (position.getZ() - anchor.getZ()) * ratio);
        }

        // La inercia real es el desplazamiento efectivo tras la restriccion.
        // Sin esto el globo no rebota al tensarse la cuerda.
        inertia.setX(position.getX() - previous.getX());
        inertia.setY(position.getY() - previous.getY());
        inertia.setZ(position.getZ() - previous.getZ());

        // El giro se alimenta del movimiento lateral, con minimo para que
        // nunca se quede completamente quieto.
        spin *= 0.98f;
        spin += (float) ((inertia.getX() - inertia.getZ()) * 8);
        if (Math.abs(spin) < 0.6f) spin = spin >= 0 ? 0.6f : -0.6f;
        position.setYaw(position.getYaw() + spin);

        model.teleport(position);
        model.headYaw(position.getYaw());
        if (leashAnchor != null && leashAnchor.isValid()) {
            leashAnchor.teleport(position);
        }

        if (balloonType.leashMode() == BalloonType.LeashMode.PARTICLE) {
            drawParticleLeash(anchor);
        }
        // El tracking de espectadores solo aplica al backend de paquetes; el
        // teleport ya lo refresca, asi que aqui no hace falta nada extra.
        if (balloonType.ambientParticle() != null && tick % balloonType.ambientInterval() == 0) {
            balloonType.ambientParticle().spawn(position.clone().add(0, 0.6, 0));
        }
    }

    private void drawParticleLeash(Location anchor) {
        Vector delta = position.toVector().subtract(anchor.toVector());
        Particles.of(ParticleType.SMOKE).count(1).speed(0)
                .spawn(anchor, Shapes.line(new Vector(0, 0, 0), delta, 0.25));
    }

    /** Punto de anclaje: el hombro del jugador, girado con su yaw. */
    private Location anchor() {
        Location base = player.getLocation();
        double yaw = Math.toRadians(-base.getYaw());
        // En 1.8 no hay mano secundaria, asi que siempre va del lado derecho.
        double side = ServerVersion.isLegacy() ? SHOULDER_OFFSET : SHOULDER_OFFSET;
        return base.clone().add(
                Math.cos(yaw) * side, balloonType.height(), Math.sin(yaw) * side);
    }

    private void teleportTo(Location location) {
        position.setWorld(location.getWorld());
        position.setX(location.getX());
        position.setY(location.getY());
        position.setZ(location.getZ());
        inertia.setX(0);
        inertia.setY(LIFT);
        inertia.setZ(0);
        if (model != null && model.alive()) model.teleport(position);
        if (leashAnchor != null && leashAnchor.isValid()) leashAnchor.teleport(position);
    }

    @Override
    protected void onUnequip() {
        if (model != null && model.alive()) {
            Location at = position.clone().add(0, 0.5, 0);
            Particles.dust(balloonType.popColor()).count(1)
                    .spawn(at, Shapes.sphere(0.35, 14));
            Sounds.POP.playAt(at, 0.6f, 0.6f);
            model.destroy();
        }
        if (leashAnchor != null && leashAnchor.isValid()) leashAnchor.remove();
        model = null;
        leashAnchor = null;
    }

    public Model model() {
        return model;
    }

    private static double noise() {
        return (ThreadLocalRandom.current().nextDouble() - 0.5) * WIND_STRENGTH;
    }
}
