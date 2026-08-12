package com.ricodevvv.aurora.particle.backend;

import com.ricodevvv.aurora.particle.ParticleBackend;
import com.ricodevvv.aurora.particle.ParticleBuilder;
import com.ricodevvv.aurora.particle.ParticleType;
import com.ricodevvv.aurora.util.Reflect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * Backend de 1.8.x. No hay Particle API en Bukkit, asi que armamos el
 * PacketPlayOutWorldParticles a mano.
 *
 * Firma en v1_8_R3:
 *   PacketPlayOutWorldParticles(EnumParticle, boolean far, float x, float y, float z,
 *                               float offX, float offY, float offZ,
 *                               float speed, int count, int... data)
 *
 * El flag "far" hace que el cliente la renderice mas alla de 16 bloques; lo
 * dejamos en true porque nosotros ya filtramos espectadores por range.
 */
public class LegacyBackend implements ParticleBackend {

    private final Map<ParticleType, Object> resolved = new EnumMap<>(ParticleType.class);

    private final Class<?> enumParticleClass;
    private final Constructor<?> packetCtor;
    private Method getMaterialId;

    public LegacyBackend() {
        enumParticleClass = Reflect.nms("EnumParticle");
        packetCtor = Reflect.constructor(Reflect.nms("PacketPlayOutWorldParticles"),
                enumParticleClass, boolean.class,
                float.class, float.class, float.class,
                float.class, float.class, float.class,
                float.class, int.class, int[].class);
        getMaterialId = Reflect.method(Material.class, "getId");

        if (packetCtor == null) {
            Bukkit.getLogger().warning("[Aurora] No pude resolver PacketPlayOutWorldParticles; particulas desactivadas.");
        }
    }

    @Override
    public boolean supports(ParticleType type) {
        return particle(type) != null;
    }

    @Override
    public void spawn(Collection<? extends Player> viewers, ParticleBuilder b,
                      double x, double y, double z) {
        if (packetCtor == null) return;

        Object particle = particle(b.type());
        if (particle == null) return;

        int count = b.count();
        float ox = (float) b.offsetX(), oy = (float) b.offsetY(), oz = (float) b.offsetZ();
        float speed = (float) b.speed();
        int[] data = new int[0];

        if (b.type().colorable()) {
            // En 1.8 el color de REDSTONE va en los offsets con count = 0 y speed = 1.
            // El rojo no puede ser 0 exacto o el cliente lo interpreta como color por defecto.
            Color c = b.color();
            count = 0;
            speed = 1f;
            ox = Math.max(c.getRed(), 1) / 255f;
            oy = c.getGreen() / 255f;
            oz = c.getBlue() / 255f;
        } else if (b.type().needsMaterial()) {
            int id = materialId(b.material());
            data = b.type().kind() == ParticleType.Kind.ITEM
                    ? new int[]{id, b.materialData()}
                    : new int[]{id + (b.materialData() << 12)};
        }

        try {
            Object packet = packetCtor.newInstance(particle, true,
                    (float) x, (float) y, (float) z,
                    ox, oy, oz, speed, count, data);
            for (Player viewer : viewers) {
                Reflect.sendPacket(viewer, packet);
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("deprecation")
    private int materialId(Material material) {
        try {
            if (getMaterialId != null) return (int) getMaterialId.invoke(material);
        } catch (Throwable ignored) {
        }
        return 1; // stone
    }

    private Object particle(ParticleType type) {
        if (resolved.containsKey(type)) return resolved.get(type);

        Object value = null;
        if (type.legacyName() != null) {
            value = Reflect.enumValue(enumParticleClass, type.legacyName());
        }
        if (value == null) {
            value = Reflect.enumValue(enumParticleClass, "FLAME");
            Bukkit.getLogger().info("[Aurora] " + type + " no existe en 1.8, usando FLAME como fallback.");
        }
        resolved.put(type, value);
        return value;
    }
}
