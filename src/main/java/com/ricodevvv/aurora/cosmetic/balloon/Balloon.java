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

    /** Amortiguacion de la inercia por tick. */
    private static final double INERTIA_DECAY = 0.95;
    /** Flotabilidad: la inercia vertical nunca baja de esto. */
    private static final double LIFT = 0.1;
    /** Inercia vertical inicial, para que el globo suba de golpe al equiparlo. */
    private static final double INITIAL_LIFT = 0.2;
    private static final double WIND_STRENGTH = 0.01;
    private static final double TURBULENCE = 0.08;
    /** Amortiguacion del giro. */
    private static final float ROTATION_DECAY = 0.98f;
    /** El globo nunca deja de girar del todo. */
    private static final float MIN_ROTATION_SPEED = 1.0f;
    /** Cuanto del movimiento lateral se convierte en giro. */
    private static final float ROTATION_MULTIPLIER = 1.0f;

    /**
     * Desplazamiento del ancla respecto al jugador, antes de rotarlo con su yaw.
     *
     * <p>La Y es CERO a proposito: el ancla va a la altura de los pies, no
     * sobre la cabeza. El globo sube solo por flotabilidad hasta que la cuerda
     * lo detiene. Anclarlo arriba lo deja casi inmovil, porque nace ya al
     * limite de la cuerda y no le queda recorrido.
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
     * Ancla de la correa: un conejo bebe invisible.
     *
     * En 1.8 la correa SOLO funciona sobre Creature, asi que ni el ArmorStand
     * ni un Display sirven. El conejo va sin IA y se teletransporta cada tick
     * a la posicion del globo, asi que visualmente la cuerda sale del globo.
     */
    private void createLeashAnchor(Location at) {
        try {
            leashAnchor = at.getWorld().spawnEntity(at, Entities.type("RABBIT"));
            // El orden importa: tag() primero, porque el listener de proteccion
            // necesita la marca desde el primer tick de vida de la entidad.
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
        // El tracking de espectadores solo aplica al backend de paquetes; el
        // teleport ya lo refresca, asi que aqui no hace falta nada extra.
        if (balloonType.ambientParticle() != null && tick % balloonType.ambientInterval() == 0) {
            balloonType.ambientParticle().spawn(position.clone().add(0, 0.6, 0));
        }
    }

    private void drawParticleLeash(Location anchor) {
        Vector delta = position.toVector().subtract(anchor.toVector());
        Particles.of(XParticle.SMOKE).count(1).speed(0)
                .spawn(anchor, Shapes.line(new Vector(0, 0, 0), delta, 0.25));
    }

    /**
     * Punto de anclaje: el costado del jugador, girado con su yaw.
     *
     * @return posicion del ancla este tick
     */
    private Location anchor() {
        Location base = player.getLocation();
        Vector offset = ANCHOR_OFFSET.clone();
        VectorMath.rotateY(offset, Math.toRadians(-base.getYaw()));
        return base.clone().add(offset.getX(),
                offset.getY() + balloonType.height(), offset.getZ());
    }

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
            // Mismo reventon que ProCosmetics: nube de 10 particulas con
            // dispersion 0.15 y velocidad 0.05, mas el sonido de huevo grave.
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

    public Model model() {
        return model;
    }

    private static double noise() {
        return (ThreadLocalRandom.current().nextDouble() - 0.5) * WIND_STRENGTH;
    }
}
