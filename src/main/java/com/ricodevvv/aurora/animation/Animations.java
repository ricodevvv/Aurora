package com.ricodevvv.aurora.animation;

import com.ricodevvv.aurora.particle.ParticleBuilder;
import com.ricodevvv.aurora.particle.ParticleType;
import com.ricodevvv.aurora.shape.Curves;
import com.ricodevvv.aurora.shape.Glyphs;
import com.ricodevvv.aurora.shape.Shape;
import com.ricodevvv.aurora.shape.Shapes;
import com.ricodevvv.aurora.util.Colors;
import com.ricodevvv.aurora.util.Easing;
import com.ricodevvv.aurora.util.VectorMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.function.Supplier;

/** Presets listos para usar. Todos devuelven la animacion SIN arrancar: llamale .start(). */
public final class Animations {

    private Animations() {
    }

    /** Sigue a una entidad y le pinta la figura encima. */
    public static Supplier<Location> follow(Entity entity, double yOffset) {
        return () -> entity.isValid() ? entity.getLocation().add(0, yOffset, 0) : null;
    }

    /** Aura girando alrededor de una entidad. */
    public static ShapeAnimation aura(Entity entity, ParticleBuilder particle, double radius, int points) {
        return new ShapeAnimation(follow(entity, 0.1), Shapes.circle(radius, points), particle)
                .spinDegrees(8);
    }

    /** Helice que sube alrededor de una entidad y se reinicia. */
    public static Animation helix(Entity entity, ParticleBuilder particle,
                                  double radius, double height, int pointsPerTick) {
        return new Animation() {
            double y = 0;

            @Override
            protected void update(long tick) {
                if (!entity.isValid()) {
                    stop();
                    return;
                }
                Location base = entity.getLocation();
                for (int i = 0; i < pointsPerTick; i++) {
                    double angle = (y / height) * Math.PI * 4 + (i * 0.4);
                    particle.spawn(base.clone().add(
                            Math.cos(angle) * radius, y, Math.sin(angle) * radius));
                    particle.spawn(base.clone().add(
                            Math.cos(angle + Math.PI) * radius, y, Math.sin(angle + Math.PI) * radius));
                }
                y += height / 20;
                if (y > height) y = 0;
            }
        };
    }

    /** Onda circular que se expande. Ideal para impactos, spawns y kill effects. */
    public static ShapeAnimation ringWave(Location center, ParticleBuilder particle,
                                          double from, double to, int ticks, int points) {
        return new ShapeAnimation(center, Shapes.circle(1, points), particle)
                .scaleOverTime(from, to, Easing.EASE_OUT_CUBIC)
                .spinDegrees(3)
                .duration(ticks);
    }

    /** Esfera que crece y se desvanece. */
    public static ShapeAnimation burst(Location center, ParticleBuilder particle,
                                       double radius, int ticks, int points) {
        return new ShapeAnimation(center, Shapes.sphere(1, points), particle)
                .scaleOverTime(0.1, radius, Easing.EASE_OUT_QUAD)
                .duration(ticks);
    }

    /** Rayo que viaja de A a B a X bloques por tick. */
    public static Animation beam(Location from, Location to, ParticleBuilder particle,
                                 double blocksPerTick, double spacing) {
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        return new Animation() {
            double travelled = 0;

            @Override
            protected void update(long tick) {
                double next = Math.min(length, travelled + blocksPerTick);
                Shape segment = Shapes.line(
                        direction.clone().normalize().multiply(travelled),
                        direction.clone().normalize().multiply(next),
                        spacing);
                particle.spawn(from, segment);
                travelled = next;
                if (travelled >= length) stop();
            }
        };
    }

    /** Rastro pegado a una entidad (flechas, proyectiles, jugadores con cosmetico). */
    public static Animation trail(Entity entity, ParticleBuilder particle, double yOffset) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                if (!entity.isValid()) {
                    stop();
                    return;
                }
                particle.spawn(entity.getLocation().add(0, yOffset, 0));
            }
        };
    }

    /** Varios "orbes" girando a distinta fase alrededor de un punto, con onda vertical. */
    public static Animation orbit(Supplier<Location> center, ParticleBuilder particle,
                                  int orbiters, double radius, double speedDeg, double wave) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                Location location = center.get();
                if (location == null) {
                    stop();
                    return;
                }
                double base = Math.toRadians(speedDeg) * tick;
                for (int i = 0; i < orbiters; i++) {
                    double angle = base + (Math.PI * 2 * i / orbiters);
                    double y = Math.sin(base * 2 + i) * wave;
                    particle.spawn(location.clone().add(
                            Math.cos(angle) * radius, y, Math.sin(angle) * radius));
                }
            }
        };
    }

    /** Vortice que gira y sube, tipo invocacion. */
    public static ShapeAnimation vortex(Location center, ParticleBuilder particle,
                                        double radius, double height, int points) {
        return new ShapeAnimation(center, Shapes.vortex(radius, height, 3, points), particle)
                .spinDegrees(12);
    }

    /** Corazon girando (para bodas, mascotas, lo que sea). */
    public static ShapeAnimation heart(Supplier<Location> center, ParticleBuilder particle, double scale) {
        return new ShapeAnimation(center, Shapes.heart(scale, 60), particle)
                .spinDegrees(4)
                .yOffset(1.2);
    }

    /** Escudo esferico alrededor de una entidad, rota en dos ejes. */
    public static ShapeAnimation shield(Entity entity, ParticleBuilder particle, double radius) {
        return new ShapeAnimation(follow(entity, 1.0), Shapes.sphere(radius, 80), particle)
                .spinDegrees(5)
                .tumble(2, 0);
    }

    /** Dibuja una figura mirando hacia donde apunta la location (ej. un slash frontal). */
    public static Animation slash(Location origin, ParticleBuilder particle, double radius, int ticks) {
        Shape arc = Shapes.arc(radius, 20, -60, 60).facing(origin);
        return new Animation() {
            @Override
            protected void update(long tick) {
                double t = tick / (double) ticks;
                particle.spawn(origin.clone().add(
                        VectorMath.lerp(new Vector(0, 0, 0), origin.getDirection().multiply(radius * 0.5), t)), arc);
            }
        }.duration(ticks);
    }

    // ------------------------------------------------------------ agregados

    /** Rayo dentado entre dos puntos; se redibuja cada frame para que se vea vivo. */
    public static Animation lightning(Location from, Location to, ParticleBuilder particle,
                                      double chaos, int ticks) {
        Vector delta = to.toVector().subtract(from.toVector());
        return new Animation() {
            @Override
            protected void update(long tick) {
                particle.spawn(from, Curves.lightning(new Vector(0, 0, 0), delta, chaos, 4));
            }
        }.duration(ticks).interval(2);
    }

    /** Rayo que cae del cielo a un punto. Sin dano, solo visual. */
    public static Animation lightningStrike(Location target, ParticleBuilder particle, double height) {
        return lightning(target.clone().add(0, height, 0), target, particle, 0.8, 12);
    }

    /** Alas de particulas pegadas a la espalda del jugador, giran con su yaw. */
    public static Animation wings(Player player, ParticleBuilder particle, double size) {
        Shape base = Shapes.wings(size, 6, 8);
        return new Animation() {
            @Override
            protected void update(long tick) {
                if (!player.isValid()) {
                    stop();
                    return;
                }
                Location location = player.getLocation();
                double yaw = Math.toRadians(-location.getYaw());
                // Batido suave: las alas se abren y cierran
                double flap = 1 + Math.sin(tick * 0.12) * 0.12;
                particle.spawn(location.clone().add(0, 1.1, 0), base.scale(flap).rotateY(yaw));
            }
        };
    }

    /** Aureola girando sobre la cabeza. */
    public static ShapeAnimation halo(Entity entity, ParticleBuilder particle, double radius) {
        return new ShapeAnimation(follow(entity, 2.3), Shapes.circle(radius, 24), particle)
                .spinDegrees(6);
    }

    /** Tornado de varias capas: mientras mas arriba, mas ancho y mas rapido. */
    public static Animation tornado(Location center, ParticleBuilder particle,
                                    double radius, double height, int layers) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                for (int layer = 0; layer < layers; layer++) {
                    double t = layer / (double) layers;
                    double y = height * t;
                    double r = radius * (0.2 + t);
                    double angle = tick * 0.25 * (1 + t) + layer;
                    for (int i = 0; i < 3; i++) {
                        double a = angle + (Math.PI * 2 * i / 3);
                        particle.spawn(center.clone().add(Math.cos(a) * r, y, Math.sin(a) * r));
                    }
                }
            }
        };
    }

    /** Explosion de fragmentos de bloque saliendo del centro. */
    public static Animation blockExplosion(Location center, Material material,
                                           double radius, int ticks, int points) {
        ParticleBuilder particle = new ParticleBuilder(ParticleType.BLOCK).material(material);
        return new ShapeAnimation(center, Shapes.cloud(1, points), particle)
                .scaleOverTime(0.2, radius, Easing.EASE_OUT_CUBIC)
                .duration(ticks);
    }

    /** Rayo continuo entre dos entidades; sigue a las dos mientras se mueven. */
    public static Animation laser(Entity from, Entity to, ParticleBuilder particle,
                                  double spacing, double yOffset) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                if (!from.isValid() || !to.isValid()) {
                    stop();
                    return;
                }
                Location a = from.getLocation().add(0, yOffset, 0);
                Location b = to.getLocation().add(0, yOffset, 0);
                particle.spawn(a, Shapes.line(new Vector(0, 0, 0),
                        b.toVector().subtract(a.toVector()), spacing));
            }
        };
    }

    /** Texto flotante hecho de particulas, girando sobre si mismo. */
    public static ShapeAnimation text(Location center, String text, ParticleBuilder particle,
                                      double pixelSize) {
        return new ShapeAnimation(center, Glyphs.text(text, pixelSize), particle)
                .spinDegrees(2);
    }

    /** Sprite ASCII flotante (usa Glyphs.HEART, SKULL, SWORD o el tuyo). */
    public static ShapeAnimation sprite(Location center, String[] rows, ParticleBuilder particle,
                                        double pixelSize) {
        return new ShapeAnimation(center, Glyphs.sprite(rows, pixelSize), particle)
                .spinDegrees(3);
    }

    /** Huellas: solo pinta cuando el jugador se mueve de verdad. */
    public static Animation footsteps(Player player, ParticleBuilder particle) {
        return new Animation() {
            private Location last;
            private boolean rightFoot;

            @Override
            protected void update(long tick) {
                if (!player.isValid()) {
                    stop();
                    return;
                }
                Location current = player.getLocation();
                if (last != null) {
                    double dx = current.getX() - last.getX();
                    double dz = current.getZ() - last.getZ();
                    if (dx * dx + dz * dz < 0.15) return; // quieto, no pintamos
                }
                double yaw = Math.toRadians(-current.getYaw());
                double side = rightFoot ? 0.22 : -0.22;
                rightFoot = !rightFoot;
                particle.spawn(current.clone().add(
                        Math.cos(yaw) * side, 0.05, Math.sin(yaw) * side));
                last = current;
            }
        }.interval(3);
    }

    /** Anillo que late entre dos radios. Bueno para marcar zonas o bordes. */
    public static Animation pulse(Location center, ParticleBuilder particle,
                                  double minRadius, double maxRadius, int period, int points) {
        Shape unit = Shapes.circle(1, points);
        return new Animation() {
            @Override
            protected void update(long tick) {
                double t = (Math.sin(tick * Math.PI * 2 / period) + 1) / 2;
                particle.spawn(center, unit.scale(minRadius + (maxRadius - minRadius) * t));
            }
        };
    }

    /** Trail que cambia de color a lo largo de una paleta (Colors.FIRE, ICE, etc). */
    public static Animation gradientTrail(Entity entity, ParticleBuilder particle,
                                          List<Color> palette, int cycleTicks, double yOffset) {
        return new Animation() {
            @Override
            protected void update(long tick) {
                if (!entity.isValid()) {
                    stop();
                    return;
                }
                particle.color(Colors.gradient(palette, (tick % cycleTicks) / (double) cycleTicks));
                particle.spawn(entity.getLocation().add(0, yOffset, 0));
            }
        };
    }

    /** Implosion: la esfera se cierra hacia el centro. Buena para teleports y absorciones. */
    public static ShapeAnimation implode(Location center, ParticleBuilder particle,
                                         double radius, int ticks, int points) {
        return new ShapeAnimation(center, Shapes.sphere(1, points), particle)
                .scaleOverTime(radius, 0.1, Easing.EASE_IN_QUAD)
                .duration(ticks);
    }

    /** Trayectoria en arco entre dos puntos, se va dibujando conforme avanza. */
    public static Animation lob(Location from, Location to, ParticleBuilder particle,
                                double height, int ticks) {
        Shape path = Curves.arcBetween(from, to, height, ticks);
        List<Vector> points = path.points();
        return new Animation() {
            @Override
            protected void update(long tick) {
                int index = (int) Math.min(tick, points.size() - 1);
                particle.spawn(from.clone().add(points.get(index)));
                if (index >= points.size() - 1) stop();
            }
        };
    }
}
