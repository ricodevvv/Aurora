package com.ricodevvv.aurora.particle;

import com.ricodevvv.aurora.Aurora;
import com.ricodevvv.aurora.shape.Shape;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * API fluida para lanzar particulas. Es mutable a proposito: en una animacion
 * reusas el mismo builder cada tick en vez de crear basura.
 *
 * <pre>
 * Particles.of(ParticleType.DUST)
 *          .color(Color.AQUA)
 *          .size(1.2f)
 *          .range(24)
 *          .spawn(loc);
 * </pre>
 */
public class ParticleBuilder implements Cloneable {

    private ParticleType type;
    private int count = 1;
    private double offsetX, offsetY, offsetZ;
    private double speed = 0;
    private Color color = Color.RED;
    private float size = 1f;
    private Material material = Material.STONE;
    private byte materialData = 0;
    private double range = 32;
    private Collection<? extends Player> viewers;

    public ParticleBuilder(ParticleType type) {
        this.type = type;
    }

    public ParticleBuilder type(ParticleType type) {
        this.type = type;
        return this;
    }

    public ParticleBuilder count(int count) {
        this.count = count;
        return this;
    }

    public ParticleBuilder offset(double x, double y, double z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        return this;
    }

    public ParticleBuilder offset(double all) {
        return offset(all, all, all);
    }

    /** En particulas normales es la velocidad; en algunas hace de "extra". */
    public ParticleBuilder speed(double speed) {
        this.speed = speed;
        return this;
    }

    /** Solo aplica a tipos COLORABLE (DUST, SPELL_MOB, NOTE). */
    public ParticleBuilder color(Color color) {
        this.color = color;
        return this;
    }

    public ParticleBuilder color(int r, int g, int b) {
        return color(Color.fromRGB(clamp(r), clamp(g), clamp(b)));
    }

    /** Color desde HSB, comodo para efectos arcoiris. */
    public ParticleBuilder hue(float hue) {
        java.awt.Color awt = java.awt.Color.getHSBColor(hue, 1f, 1f);
        return color(awt.getRed(), awt.getGreen(), awt.getBlue());
    }

    /** Tamano del dust. Solo 1.13+; en versiones viejas se ignora. */
    public ParticleBuilder size(float size) {
        this.size = size;
        return this;
    }

    public ParticleBuilder material(Material material) {
        this.material = material;
        return this;
    }

    public ParticleBuilder material(Material material, byte data) {
        this.material = material;
        this.materialData = data;
        return this;
    }

    /** Radio en bloques para elegir a quien se le manda. Default 32. */
    public ParticleBuilder range(double range) {
        this.range = range;
        return this;
    }

    /** Fija los espectadores a mano (util para efectos privados de un jugador). */
    public ParticleBuilder viewers(Collection<? extends Player> viewers) {
        this.viewers = viewers;
        return this;
    }

    public ParticleBuilder viewer(Player player) {
        List<Player> list = new ArrayList<>(1);
        list.add(player);
        this.viewers = list;
        return this;
    }

    /** Vuelve a modo automatico (todos los jugadores dentro del range). */
    public ParticleBuilder allNearby() {
        this.viewers = null;
        return this;
    }

    // ---------------------------------------------------------------- spawn

    public void spawn(Location location) {
        spawn(location.getWorld(), location.getX(), location.getY(), location.getZ());
    }

    public void spawn(World world, double x, double y, double z) {
        Collection<? extends Player> targets = viewers != null ? viewers : nearby(world, x, y, z);
        if (targets.isEmpty()) return;
        Aurora.backend().spawn(targets, this, x, y, z);
    }

    /** Dibuja una figura completa usando el origen dado como centro. */
    public void spawn(Location origin, Shape shape) {
        World world = origin.getWorld();
        Collection<? extends Player> targets = viewers != null
                ? viewers
                : nearby(world, origin.getX(), origin.getY(), origin.getZ());
        if (targets.isEmpty()) return;

        for (Vector point : shape.points()) {
            Aurora.backend().spawn(targets, this,
                    origin.getX() + point.getX(),
                    origin.getY() + point.getY(),
                    origin.getZ() + point.getZ());
        }
    }

    private Collection<? extends Player> nearby(World world, double x, double y, double z) {
        List<Player> found = new ArrayList<>();
        double sq = range * range;
        for (Player player : world.getPlayers()) {
            Location l = player.getLocation();
            double dx = l.getX() - x, dy = l.getY() - y, dz = l.getZ() - z;
            if (dx * dx + dy * dy + dz * dz <= sq) found.add(player);
        }
        return found;
    }

    // --------------------------------------------------------------- getters

    public ParticleType type() {
        return type;
    }

    public int count() {
        return count;
    }

    public double offsetX() {
        return offsetX;
    }

    public double offsetY() {
        return offsetY;
    }

    public double offsetZ() {
        return offsetZ;
    }

    public double speed() {
        return speed;
    }

    public Color color() {
        return color;
    }

    public float size() {
        return size;
    }

    public Material material() {
        return material;
    }

    public byte materialData() {
        return materialData;
    }

    @Override
    public ParticleBuilder clone() {
        try {
            return (ParticleBuilder) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
